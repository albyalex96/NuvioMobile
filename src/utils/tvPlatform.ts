/**
 * TV Platform Utilities
 * 
 * This module provides utilities for handling TV-specific behavior
 * on tvOS and Android TV platforms.
 */

import { Platform } from 'react-native';

/**
 * Check if the app is running on a TV platform (tvOS or Android TV)
 */
export const isTV = Platform.isTV;

/**
 * Check if the app is running on tvOS specifically
 */
export const isTVOS = Platform.OS === 'ios' && Platform.isTV;

/**
 * Check if the app is running on Android TV specifically
 */
export const isAndroidTV = Platform.OS === 'android' && Platform.isTV;

/**
 * Features that are NOT supported on TV platforms
 */
export const unsupportedTVFeatures = {
    // Push notifications are not available on TV
    pushNotifications: true,
    // Haptic feedback doesn't exist on TV
    haptics: true,
    // Brightness control doesn't make sense on TV
    brightnessControl: true,
    // Casting from TV doesn't make sense (you're already on TV)
    casting: true,
    // Device orientation doesn't apply to TV
    orientationLock: true,
    // Touch gestures need to be adapted for D-pad/remote
    touchGestures: true,
} as const;

/**
 * Safely execute a function only on non-TV platforms
 * Returns undefined if on TV, otherwise returns the function result
 */
export function runIfNotTV<T>(fn: () => T): T | undefined {
    if (isTV) return undefined;
    return fn();
}

/**
 * Safely execute an async function only on non-TV platforms
 * Returns undefined if on TV, otherwise returns the awaited result
 */
export async function runIfNotTVAsync<T>(fn: () => Promise<T>): Promise<T | undefined> {
    if (isTV) return undefined;
    return fn();
}

/**
 * Get a TV-safe value, returning the fallback on TV platforms
 */
export function tvSafeValue<T>(value: T, tvFallback: T): T {
    return isTV ? tvFallback : value;
}

/**
 * Log messages about TV platform limitations (development only)
 */
export function logTVUnsupported(feature: string): void {
    if (__DEV__ && isTV) {
        console.log(`[TV] Feature "${feature}" is not supported on TV platforms`);
    }
}
