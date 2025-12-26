import React from 'react';
import { View, Text, Pressable, StyleSheet, useWindowDimensions, Platform } from 'react-native';
import { MaterialIcons } from '@expo/vector-icons';
import Animated, {
    FadeIn,
    FadeOut,
    ZoomIn,
    ZoomOut,
} from 'react-native-reanimated';
import { FocusableTouchableOpacity } from '../../common/FocusableTouchableOpacity';

// Check if running on TV platform
const isTV = Platform.isTV;

// Conditionally import expo-clipboard (not available on TV)
let ExpoClipboard: typeof import('expo-clipboard') | null = null;
if (!isTV) {
    try {
        ExpoClipboard = require('expo-clipboard');
    } catch (e) {
        // Silently fail - copy functionality won't be available
    }
}

interface ErrorModalProps {
    showErrorModal: boolean;
    setShowErrorModal: (show: boolean) => void;
    errorDetails: string;
    onDismiss?: () => void;
}

export const ErrorModal: React.FC<ErrorModalProps> = ({
    showErrorModal,
    setShowErrorModal,
    errorDetails,
    onDismiss,
}) => {
    const [copied, setCopied] = React.useState(false);
    const { width } = useWindowDimensions();
    const MODAL_WIDTH = Math.min(width * 0.8, 400);

    const handleClose = () => {
        setShowErrorModal(false);
        if (onDismiss) {
            onDismiss();
        }
    };

    const handleCopy = async () => {
        // Skip on TV or if clipboard is not available
        if (!ExpoClipboard) return;

        await ExpoClipboard.setStringAsync(errorDetails);
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
    };

    if (!showErrorModal) return null;

    return (
        <View style={[StyleSheet.absoluteFill, { zIndex: 99999, justifyContent: 'center', alignItems: 'center' }]}>
            <Pressable style={StyleSheet.absoluteFill} onPress={handleClose} focusable={false}>
                <Animated.View entering={FadeIn.duration(200)} exiting={FadeOut.duration(150)} style={{ flex: 1, backgroundColor: 'rgba(0,0,0,0.7)' }} />
            </Pressable>

            <Animated.View
                entering={FadeIn.duration(300)}
                exiting={FadeOut.duration(200)}
                style={{
                    width: MODAL_WIDTH,
                    backgroundColor: '#1a1a1a',
                    borderRadius: 20,
                    padding: 24,
                    alignItems: 'center',
                    borderWidth: 1,
                    borderColor: 'rgba(255,255,255,0.1)',
                }}
            >
                <View style={{
                    width: 56,
                    height: 56,
                    borderRadius: 28,
                    backgroundColor: 'rgba(239, 68, 68, 0.1)',
                    alignItems: 'center',
                    justifyContent: 'center',
                    marginBottom: 16
                }}>
                    <MaterialIcons name="error-outline" size={32} color="#EF4444" />
                </View>

                <Text style={{
                    color: 'white',
                    fontSize: 20,
                    fontWeight: '700',
                    marginBottom: 8,
                    textAlign: 'center'
                }}>
                    Playback Error
                </Text>

                <Text
                    numberOfLines={3}
                    ellipsizeMode="tail"
                    style={{
                        color: 'rgba(255,255,255,0.7)',
                        fontSize: 15,
                        textAlign: 'center',
                        marginBottom: 16,
                        lineHeight: 22
                    }}
                >
                    {errorDetails || 'An unknown error occurred during playback.'}
                </Text>

                {!!ExpoClipboard && (
                    <FocusableTouchableOpacity
                        onPress={handleCopy}
                        activeOpacity={0.9}
                        enableTVFocus={Platform.isTV}
                        preset="pill"
                        focusBorderRadius={12}
                        style={{
                            flexDirection: 'row',
                            alignItems: 'center',
                            justifyContent: 'center',
                            padding: 8,
                            marginBottom: 24,
                            opacity: 0.9
                        }}
                    >
                        <MaterialIcons
                            name={copied ? "check" : "content-copy"}
                            size={16}
                            color="rgba(255,255,255,0.6)"
                            style={{ marginRight: 6 }}
                        />
                        <Text style={{ color: 'rgba(255,255,255,0.6)', fontSize: 13, fontWeight: '500' }}>
                            {copied ? 'Copied to clipboard' : 'Copy error details'}
                        </Text>
                    </FocusableTouchableOpacity>
                )}

                <FocusableTouchableOpacity
                    style={{
                        backgroundColor: 'white',
                        paddingVertical: 12,
                        paddingHorizontal: 32,
                        borderRadius: 12,
                        width: '100%',
                        alignItems: 'center'
                    }}
                    onPress={handleClose}
                    activeOpacity={0.9}
                    enableTVFocus={Platform.isTV}
                    preset="button"
                    focusBorderRadius={12}
                    hasTVPreferredFocus={Platform.isTV}
                >
                    <Text style={{
                        color: 'black',
                        fontSize: 16,
                        fontWeight: '700'
                    }}>
                        Dismiss
                    </Text>
                </FocusableTouchableOpacity>
            </Animated.View>
        </View>
    );
};

export default ErrorModal;
