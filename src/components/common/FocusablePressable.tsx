import React, { useCallback, useMemo, useRef, useState } from 'react';
import {
  Animated,
  Easing,
  Platform,
  Pressable,
  PressableProps,
  StyleProp,
  StyleSheet,
  View,
  ViewStyle,
} from 'react-native';
import { useTheme } from '../../contexts/ThemeContext';
import { tvFocusPresets, TVFocusPresetName } from '../../styles/tvFocus';

export type FocusablePressableProps = Omit<PressableProps, 'style'> & {
  containerStyle?: StyleProp<ViewStyle>;
  style?: StyleProp<ViewStyle> | ((state: { pressed: boolean }) => StyleProp<ViewStyle>);
  preset?: TVFocusPresetName;
  enableTVFocus?: boolean;
  focusBorderRadius?: number;
  focusScale?: number;
  focusRingWidth?: number;
  focusRingColor?: string;
};

export const FocusablePressable: React.FC<FocusablePressableProps> = ({
  enableTVFocus = Platform.isTV,
  containerStyle,
  style,
  preset,
  focusBorderRadius,
  focusScale,
  focusRingWidth,
  focusRingColor,
  onFocus,
  onBlur,
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

  return (
    <Animated.View
      style={[
        containerStyle,
        containerAnimatedStyle,
        isFocused && enableTVFocus && {
          shadowColor: ringColor,
          shadowOpacity: 0.55,
          shadowRadius: 14,
          shadowOffset: { width: 0, height: 0 },
          elevation: 14,
        },
      ]}
    >
      <Pressable
        {...rest}
        focusable={rest.focusable ?? enableTVFocus}
        onFocus={handleFocus}
        onBlur={handleBlur}
        style={(state) => {
          const base = typeof style === 'function' ? style({ pressed: state.pressed }) : style;
          return [base, { position: 'relative' } as ViewStyle] as any;
        }}
      >
        {children}
        {enableTVFocus && (
          <>
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
          </>
        )}
      </Pressable>
    </Animated.View>
  );
};

const styles = StyleSheet.create({
  focusRing: {
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
  },
});

