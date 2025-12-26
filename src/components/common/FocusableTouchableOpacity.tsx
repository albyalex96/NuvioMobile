import React, { useCallback, useMemo, useRef, useState } from 'react';
import {
  Animated,
  Easing,
  Platform,
  StyleProp,
  StyleSheet,
  TouchableOpacity,
  TouchableOpacityProps,
  View,
  ViewStyle,
} from 'react-native';
import { useTheme } from '../../contexts/ThemeContext';
import { tvFocusPresets, TVFocusPresetName } from '../../styles/tvFocus';

export type FocusableTouchableOpacityProps = Omit<TouchableOpacityProps, 'style'> & {
  /**
   * Optional style applied to the outer Animated wrapper.
   * Useful when the touchable itself is absolutely positioned (e.g. overlays).
   */
  containerStyle?: StyleProp<ViewStyle>;
  style?: StyleProp<ViewStyle>;
  /** Optional preset to standardize focus behavior across the app. */
  preset?: TVFocusPresetName;
  /**
   * When true, focus visuals are enabled (defaults to Platform.isTV).
   * You can force-enable for large-screen non-TV if desired.
   */
  enableTVFocus?: boolean;
  /** Border radius for the focus ring overlay. */
  focusBorderRadius?: number;
  /** Scale applied when focused. */
  focusScale?: number;
  /** Focus ring thickness (overlay, doesn't affect layout). */
  focusRingWidth?: number;
  /** Focus ring color (defaults to theme primary). */
  focusRingColor?: string;
};

export const FocusableTouchableOpacity: React.FC<FocusableTouchableOpacityProps> = ({
  enableTVFocus = Platform.isTV,
  containerStyle,
  preset,
  focusBorderRadius,
  focusScale,
  focusRingWidth,
  focusRingColor,
  onFocus,
  onBlur,
  activeOpacity,
  style,
  children,
  ...rest
}) => {
  const { currentTheme } = useTheme();
  const ringColor = focusRingColor ?? currentTheme.colors.primary;

  const resolvedPreset = preset ? tvFocusPresets[preset] : undefined;
  const resolvedBorderRadius = focusBorderRadius ?? resolvedPreset?.focusBorderRadius ?? 12;
  const resolvedScale = focusScale ?? resolvedPreset?.focusScale ?? 1.06;
  const resolvedRingWidth = focusRingWidth ?? resolvedPreset?.focusRingWidth ?? 3;

  const focusProgress = useRef(new Animated.Value(0)).current;
  const [isFocused, setIsFocused] = useState(false);

  const animateTo = useCallback((toValue: 0 | 1) => {
    Animated.timing(focusProgress, {
      toValue,
      duration: 140,
      easing: Easing.out(Easing.cubic),
      useNativeDriver: true,
    }).start();
  }, [focusProgress]);

  const handleFocus = useCallback((e: any) => {
    if (enableTVFocus) {
      setIsFocused(true);
      animateTo(1);
    }
    onFocus?.(e);
  }, [enableTVFocus, animateTo, onFocus]);

  const handleBlur = useCallback((e: any) => {
    if (enableTVFocus) {
      setIsFocused(false);
      animateTo(0);
    }
    onBlur?.(e);
  }, [enableTVFocus, animateTo, onBlur]);

  const containerAnimatedStyle = useMemo(() => {
    if (!enableTVFocus) return null;
    return {
      transform: [
        {
          scale: focusProgress.interpolate({
            inputRange: [0, 1],
            outputRange: [1, resolvedScale],
          }),
        },
      ],
    } as any;
  }, [enableTVFocus, focusProgress, resolvedScale]);

  const ringAnimatedStyle = useMemo(() => {
    if (!enableTVFocus) return null;
    return {
      opacity: focusProgress,
      transform: [
        {
          scale: focusProgress.interpolate({
            inputRange: [0, 1],
            outputRange: [0.985, 1],
          }),
        },
      ],
    } as any;
  }, [enableTVFocus, focusProgress]);

  // Avoid the default "dim" feel on TV by not changing opacity on press.
  const finalActiveOpacity = enableTVFocus ? 1 : (activeOpacity ?? 0.7);

  if (!enableTVFocus) {
    return (
      <TouchableOpacity {...rest} activeOpacity={finalActiveOpacity} style={style} onFocus={handleFocus} onBlur={handleBlur}>
        {children}
      </TouchableOpacity>
    );
  }

  return (
    <Animated.View
      style={[
        containerStyle,
        containerAnimatedStyle,
        isFocused && {
          shadowColor: ringColor,
          shadowOpacity: 0.55,
          shadowRadius: 14,
          shadowOffset: { width: 0, height: 0 },
          elevation: 14,
        },
      ]}
    >
      <TouchableOpacity
        {...rest}
        focusable={rest.focusable ?? enableTVFocus}
        activeOpacity={finalActiveOpacity}
        onFocus={handleFocus}
        onBlur={handleBlur}
        style={[style, { position: 'relative' } as ViewStyle]}
      >
        {children}
        <Animated.View
          pointerEvents="none"
          style={[
            StyleSheet.absoluteFillObject,
            styles.focusRing,
            {
              borderColor: ringColor,
              borderWidth: resolvedRingWidth,
              borderRadius: resolvedBorderRadius,
            },
            ringAnimatedStyle,
          ]}
        />
        {/* Slight inner highlight to make focus readable on very bright posters */}
        <View
          pointerEvents="none"
          style={[
            StyleSheet.absoluteFillObject,
            {
              borderRadius: resolvedBorderRadius,
              backgroundColor: isFocused ? 'rgba(255,255,255,0.06)' : 'transparent',
            },
          ]}
        />
      </TouchableOpacity>
    </Animated.View>
  );
};

const styles = StyleSheet.create({
  focusRing: {
    // Keep ring inside bounds to avoid overflow clipping.
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
  },
});

