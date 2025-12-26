import { useEffect, useRef, useCallback } from 'react';
import { StatusBar, Dimensions, AppState, InteractionManager, Platform } from 'react-native';
import { activateKeepAwakeAsync, deactivateKeepAwake } from 'expo-keep-awake';
import { logger } from '../../../utils/logger';
import { useFocusEffect } from '@react-navigation/native';

// Check if running on TV
const isTV = Platform.isTV;

// Conditionally import Brightness and ScreenOrientation (not available on TV)
let Brightness: typeof import('expo-brightness') | null = null;
let ScreenOrientation: typeof import('expo-screen-orientation') | null = null;

if (!isTV) {
    try {
        Brightness = require('expo-brightness');
        ScreenOrientation = require('expo-screen-orientation');
    } catch (e) {
        logger.warn('[usePlayerSetup] Brightness/ScreenOrientation not available:', e);
    }
}

interface PlayerSetupConfig {
    setScreenDimensions: (dim: any) => void;
    setVolume: (vol: number) => void;
    setBrightness: (bri: number) => void;
    isOpeningAnimationComplete: boolean;
    paused: boolean;
}

export const usePlayerSetup = (config: PlayerSetupConfig) => {
    const {
        setScreenDimensions,
        setVolume,
        setBrightness,
        isOpeningAnimationComplete,
        paused
    } = config;

    // Prevent screen sleep while playing
    // Prevent screen sleep while playing
    useEffect(() => {
        if (!paused) {
            activateKeepAwakeAsync();
        } else {
            deactivateKeepAwake();
        }
        return () => {
            deactivateKeepAwake();
        };
    }, [paused]);

    const isAppBackgrounded = useRef(false);

    const enableImmersiveMode = () => {
        StatusBar.setHidden(true, 'none');
    };

    const disableImmersiveMode = () => {
        StatusBar.setHidden(false, 'fade');
    };

    useFocusEffect(
        useCallback(() => {
            if (isOpeningAnimationComplete) {
                enableImmersiveMode();
            }
            return () => { };
        }, [isOpeningAnimationComplete])
    );

    useEffect(() => {
        // Initial Setup
        const subscription = Dimensions.addEventListener('change', ({ screen }) => {
            setScreenDimensions(screen);
            if (isOpeningAnimationComplete) {
                enableImmersiveMode();
            }
        });

        StatusBar.setHidden(true, 'none');
        if (isOpeningAnimationComplete) {
            enableImmersiveMode();
        }

        // Initialize volume (normalized 0-1 for cross-platform)
        setVolume(1.0);

        // Initialize Brightness (skip on TV)
        const initBrightness = () => {
            if (!Brightness) {
                setBrightness(1.0);
                return;
            }
            InteractionManager.runAfterInteractions(async () => {
                try {
                    const currentBrightness = await Brightness!.getBrightnessAsync();
                    setBrightness(currentBrightness);
                } catch (error) {
                    logger.warn('[usePlayerSetup] Error getting initial brightness:', error);
                    setBrightness(1.0);
                }
            });
        };
        initBrightness();

        return () => {
            subscription?.remove();
            disableImmersiveMode();
        };
    }, [isOpeningAnimationComplete]);

    const orientationLocked = useRef(false);

    useEffect(() => {
        // Skip orientation lock on TV (not needed)
        if (!ScreenOrientation) return;

        if (isOpeningAnimationComplete && !orientationLocked.current) {
            const task = InteractionManager.runAfterInteractions(() => {
                ScreenOrientation!.lockAsync(ScreenOrientation!.OrientationLock.LANDSCAPE)
                    .then(() => {
                        orientationLocked.current = true;
                    })
                    .catch(() => { });
            });
            return () => task.cancel();
        }
    }, [isOpeningAnimationComplete]);

    useEffect(() => {
        return () => {
            // Skip on TV
            if (!ScreenOrientation) return;

            ScreenOrientation.lockAsync(ScreenOrientation.OrientationLock.DEFAULT)
                .then(() => ScreenOrientation!.unlockAsync())
                .catch(() => { });
        };
    }, []);

    // Handle App State
    useEffect(() => {
        const onAppStateChange = (state: string) => {
            if (state === 'active') {
                isAppBackgrounded.current = false;
                if (isOpeningAnimationComplete) {
                    enableImmersiveMode();
                }
            } else if (state === 'background' || state === 'inactive') {
                isAppBackgrounded.current = true;
            }
        };
        const sub = AppState.addEventListener('change', onAppStateChange);
        return () => sub.remove();
    }, [isOpeningAnimationComplete]);

    return { isAppBackgrounded };
};
