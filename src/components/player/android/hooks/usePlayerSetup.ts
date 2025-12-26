import { useEffect, useRef } from 'react';
import { StatusBar, Platform, Dimensions, AppState } from 'react-native';
import { activateKeepAwakeAsync, deactivateKeepAwake } from 'expo-keep-awake';
import { logger } from '../../../../utils/logger';
import { useFocusEffect } from '@react-navigation/native';
import { useCallback } from 'react';

// Check if running on TV
const isTV = Platform.isTV;

// Conditionally import modules not available on Android TV
let RNImmersiveMode: any = null;
let NavigationBar: typeof import('expo-navigation-bar') | null = null;
let Brightness: typeof import('expo-brightness') | null = null;

if (!isTV) {
    try {
        RNImmersiveMode = require('react-native-immersive-mode').default;
        NavigationBar = require('expo-navigation-bar');
        Brightness = require('expo-brightness');
    } catch (e) {
        logger.warn('[usePlayerSetup] Some player modules not available:', e);
    }
}

const DEBUG_MODE = false;

export const usePlayerSetup = (
    setScreenDimensions: (dim: any) => void,
    setVolume: (vol: number) => void,
    setBrightness: (bri: number) => void,
    paused: boolean
) => {
    const originalSystemBrightnessRef = useRef<number | null>(null);
    const originalSystemBrightnessModeRef = useRef<number | null>(null);
    const isAppBackgrounded = useRef(false);

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

    const enableImmersiveMode = async () => {
        if (Platform.OS === 'android' && !isTV) {
            // Standard immersive mode (not available on TV)
            if (RNImmersiveMode) {
                RNImmersiveMode.setBarTranslucent(true);
                RNImmersiveMode.fullLayout(true);
            }
            StatusBar.setHidden(true, 'none');

            // Explicitly hide bottom navigation bar using Expo
            if (NavigationBar) {
                try {
                    await NavigationBar.setVisibilityAsync("hidden");
                    await NavigationBar.setBehaviorAsync("overlay-swipe");
                } catch (e) {
                    // Ignore errors on non-supported devices
                }
            }
        }
    };

    const disableImmersiveMode = async () => {
        if (Platform.OS === 'android' && !isTV) {
            if (RNImmersiveMode) {
                RNImmersiveMode.setBarTranslucent(false);
                RNImmersiveMode.fullLayout(false);
            }
            StatusBar.setHidden(false, 'fade');

            if (NavigationBar) {
                try {
                    await NavigationBar.setVisibilityAsync("visible");
                } catch (e) {
                    // Ignore
                }
            }
        }
    };

    useFocusEffect(
        useCallback(() => {
            enableImmersiveMode();
            return () => { };
        }, [])
    );

    useEffect(() => {
        // Initial Setup
        const subscription = Dimensions.addEventListener('change', ({ screen }) => {
            setScreenDimensions(screen);
            enableImmersiveMode();
        });

        StatusBar.setHidden(true, 'none');
        enableImmersiveMode();

        // Initialize volume (default to 1.0)
        setVolume(1.0);

        // Initialize Brightness (skip on TV)
        const initBrightness = async () => {
            if (!Brightness) {
                setBrightness(1.0);
                return;
            }
            try {
                if (Platform.OS === 'android' && !isTV) {
                    try {
                        const [sysBright, sysMode] = await Promise.all([
                            (Brightness as any).getSystemBrightnessAsync?.(),
                            (Brightness as any).getSystemBrightnessModeAsync?.()
                        ]);
                        originalSystemBrightnessRef.current = typeof sysBright === 'number' ? sysBright : null;
                        originalSystemBrightnessModeRef.current = typeof sysMode === 'number' ? sysMode : null;
                    } catch (e) {
                        // ignore
                    }
                }
                const currentBrightness = await Brightness.getBrightnessAsync();
                setBrightness(currentBrightness);
            } catch (error) {
                logger.warn('[usePlayerSetup] Error setting brightness', error);
                setBrightness(1.0);
            }
        };
        initBrightness();

        return () => {
            subscription?.remove();
            disableImmersiveMode();

            // Restore brightness on unmount
            if (Platform.OS === 'android' && originalSystemBrightnessRef.current !== null) {
                // restoration logic normally happens here or in a separate effect
            }
        };
    }, []);

    // Handle App State
    useEffect(() => {
        const onAppStateChange = (state: string) => {
            if (state === 'active') {
                isAppBackgrounded.current = false;
                enableImmersiveMode();
            } else if (state === 'background' || state === 'inactive') {
                isAppBackgrounded.current = true;
            }
        };
        const sub = AppState.addEventListener('change', onAppStateChange);
        return () => sub.remove();
    }, []);

    return { isAppBackgrounded };
};
