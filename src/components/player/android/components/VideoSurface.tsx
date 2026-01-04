import React, { useCallback, useRef, forwardRef, useImperativeHandle, useEffect, useState } from 'react';
import { View, TouchableWithoutFeedback, StyleSheet, Platform } from 'react-native';
import { PinchGestureHandler } from 'react-native-gesture-handler';
import { styles } from '../../utils/playerStyles';
import { ResizeModeType } from '../../utils/playerTypes';
import { logger } from '../../../../utils/logger';

// Conditionally import native-only modules
const Video = Platform.OS !== 'web' ? require('react-native-video').default : null;
const ResizeMode = Platform.OS !== 'web' ? require('react-native-video').ResizeMode : null;
const MpvPlayer = Platform.OS !== 'web' ? require('../MpvPlayer').default : null;

// Type imports for TypeScript (these don't cause runtime imports)
import type { VideoRef, SelectedTrack, SelectedVideoTrack, ResizeMode as ResizeModeEnum } from 'react-native-video';
import type { MpvPlayerRef } from '../MpvPlayer';

// Codec error patterns that indicate we should fallback to MPV
const CODEC_ERROR_PATTERNS = [
    'exceeds_capabilities',
    'no_exceeds_capabilities',
    'decoder_exception',
    'decoder.*error',
    'codec.*error',
    'unsupported.*codec',
    'mediacodec.*exception',
    'omx.*error',
    'dolby.*vision',
    'hevc.*error',
    'no suitable decoder',
    'decoder initialization failed',
    'format.no_decoder',
    'no_decoder',
    'decoding_failed',
    'error_code_decoding',
    'exoplaybackexception',
    'mediacodecvideodecoder',
    'mediacodecvideodecoderexception',
    'decoder failed',
];

interface VideoSurfaceProps {
    processedStreamUrl: string;
    headers?: { [key: string]: string };
    volume: number;
    playbackSpeed: number;
    resizeMode: ResizeModeType;
    paused: boolean;
    currentStreamUrl: string;

    // Callbacks
    toggleControls: () => void;
    onLoad: (data: any) => void;
    onProgress: (data: any) => void;
    onSeek: (data: any) => void;
    onEnd: () => void;
    onError: (err: any) => void;
    onBuffer: (buf: any) => void;

    // Refs
    mpvPlayerRef?: React.RefObject<MpvPlayerRef>;
    exoPlayerRef?: React.RefObject<VideoRef>;
    pinchRef: any;

    // Handlers
    onPinchGestureEvent: any;
    onPinchHandlerStateChange: any;
    screenDimensions: { width: number, height: number };
    onTracksChanged?: (data: { audioTracks: any[]; subtitleTracks: any[] }) => void;
    selectedAudioTrack?: SelectedTrack;
    selectedTextTrack?: SelectedTrack;
    decoderMode?: 'auto' | 'sw' | 'hw' | 'hw+';
    gpuMode?: 'gpu' | 'gpu-next';

    // Dual Engine Props
    useExoPlayer?: boolean;
    onCodecError?: () => void;
    onEngineChange?: (engine: 'exoplayer' | 'mpv') => void;

    // Web video ref for seeking
    webVideoRef?: React.RefObject<HTMLVideoElement>;

    // Subtitle Styling
    subtitleSize?: number;
    subtitleColor?: string;
    subtitleBackgroundOpacity?: number;
    subtitleBorderSize?: number;
    subtitleBorderColor?: string;
    subtitleShadowEnabled?: boolean;
    subtitlePosition?: number;
    subtitleDelay?: number;
    subtitleAlignment?: 'left' | 'center' | 'right';
}

// Helper function to check if error is a codec error
const isCodecError = (errorString: string): boolean => {
    const lowerError = errorString.toLowerCase();
    return CODEC_ERROR_PATTERNS.some(pattern => {
        const regex = new RegExp(pattern, 'i');
        return regex.test(lowerError);
    });
};

export const VideoSurface: React.FC<VideoSurfaceProps> = ({
    processedStreamUrl,
    headers,
    volume,
    playbackSpeed,
    resizeMode,
    paused,
    currentStreamUrl,
    toggleControls,
    onLoad,
    onProgress,
    onSeek,
    onEnd,
    onError,
    onBuffer,
    mpvPlayerRef,
    exoPlayerRef,
    pinchRef,
    onPinchGestureEvent,
    onPinchHandlerStateChange,
    screenDimensions,
    onTracksChanged,
    selectedAudioTrack,
    selectedTextTrack,
    decoderMode,
    gpuMode,
    // Dual Engine
    useExoPlayer = true,
    onCodecError,
    onEngineChange,
    // Subtitle Styling
    subtitleSize,
    subtitleColor,
    subtitleBackgroundOpacity,
    subtitleBorderSize,
    subtitleBorderColor,
    subtitleShadowEnabled,
    subtitlePosition,
    subtitleDelay,
    subtitleAlignment,
    webVideoRef,
}) => {
    // Use the actual stream URL
    const streamUrl = currentStreamUrl || processedStreamUrl;

    // ========== MPV Handlers ==========
    const handleMpvLoad = (data: { duration: number; width: number; height: number }) => {
        console.log('[VideoSurface] MPV onLoad received:', data);
        onLoad({
            duration: data.duration,
            naturalSize: {
                width: data.width,
                height: data.height,
            },
        });
    };

    const handleMpvProgress = (data: { currentTime: number; duration: number }) => {
        onProgress({
            currentTime: data.currentTime,
            playableDuration: data.currentTime,
        });
    };

    const handleMpvError = (error: { error: string }) => {
        console.log('[VideoSurface] MPV onError received:', error);
        onError({
            error: {
                errorString: error.error,
            },
        });
    };

    const handleMpvEnd = () => {
        console.log('[VideoSurface] MPV onEnd received');
        onEnd();
    };

    // ========== ExoPlayer Handlers ==========
    const handleExoLoad = (data: any) => {
        console.log('[VideoSurface] ExoPlayer onLoad received:', data);
        console.log('[VideoSurface] ExoPlayer textTracks raw:', JSON.stringify(data.textTracks, null, 2));

        // Extract track information
        const audioTracks = data.audioTracks?.map((t: any, i: number) => ({
            id: t.index ?? i,
            name: t.title || t.language || `Track ${i + 1}`,
            language: t.language,
        })) ?? [];

        const subtitleTracks = data.textTracks?.map((t: any, i: number) => {
            const track = {
                id: t.index ?? i,
                name: t.title || t.language || `Track ${i + 1}`,
                language: t.language,
            };
            console.log('[VideoSurface] Mapped subtitle track:', track, 'original:', t);
            return track;
        }) ?? [];

        if (onTracksChanged && (audioTracks.length > 0 || subtitleTracks.length > 0)) {
            onTracksChanged({ audioTracks, subtitleTracks });
        }

        onLoad({
            duration: data.duration,
            naturalSize: data.naturalSize || { width: 1920, height: 1080 },
            audioTracks: data.audioTracks,
            textTracks: data.textTracks,
        });
    };

    const handleExoProgress = (data: any) => {
        onProgress({
            currentTime: data.currentTime,
            playableDuration: data.playableDuration || data.currentTime,
        });
    };

    const handleExoError = (error: any) => {
        console.log('[VideoSurface] ExoPlayer onError received:', JSON.stringify(error, null, 2));

        // Extract error string - try multiple paths
        let errorString = 'Unknown error';
        const errorParts: string[] = [];

        if (typeof error?.error === 'string') {
            errorParts.push(error.error);
        }
        if (error?.error?.errorString) {
            errorParts.push(error.error.errorString);
        }
        if (error?.error?.errorCode) {
            errorParts.push(String(error.error.errorCode));
        }
        if (typeof error === 'string') {
            errorParts.push(error);
        }
        if (error?.nativeStackAndroid) {
            errorParts.push(error.nativeStackAndroid.join(' '));
        }
        if (error?.message) {
            errorParts.push(error.message);
        }

        // Combine all error parts for comprehensive checking
        errorString = errorParts.length > 0 ? errorParts.join(' ') : JSON.stringify(error);

        console.log('[VideoSurface] Extracted error string:', errorString);
        console.log('[VideoSurface] isCodecError result:', isCodecError(errorString));

        // Check if this is a codec error that should trigger fallback
        if (isCodecError(errorString)) {
            logger.warn('[VideoSurface] ExoPlayer codec error detected, triggering MPV fallback:', errorString);
            onCodecError?.();
            return; // Don't propagate codec errors - we're falling back silently
        }

        // Non-codec errors should be propagated
        onError({
            error: {
                errorString: errorString,
            },
        });
    };

    const handleExoBuffer = (data: any) => {
        onBuffer({ isBuffering: data.isBuffering });
    };

    const handleExoEnd = () => {
        console.log('[VideoSurface] ExoPlayer onEnd received');
        onEnd();
    };

    const handleExoSeek = (data: any) => {
        onSeek({ currentTime: data.currentTime });
    };

    // Map ResizeModeType to react-native-video ResizeMode
    const getExoResizeMode = (): any => {
        if (!ResizeMode) return 'contain'; // Fallback for web
        switch (resizeMode) {
            case 'cover':
                return ResizeMode.COVER;
            case 'stretch':
                return ResizeMode.STRETCH;
            case 'contain':
            default:
                return ResizeMode.CONTAIN;
        }
    };

    // Map ResizeModeType to CSS object-fit for web
    const getWebObjectFit = (): 'contain' | 'cover' | 'fill' => {
        switch (resizeMode) {
            case 'cover':
                return 'cover';
            case 'stretch':
                return 'fill';
            case 'contain':
            default:
                return 'contain';
        }
    };

    // Web video progress interval ref
    const webProgressIntervalRef = useRef<any>(null);

    // Web video event handlers and effects
    useEffect(() => {
        if (Platform.OS !== 'web') return;

        const video = webVideoRef?.current;
        if (!video) return;

        // Handle load metadata
        const handleLoadedMetadata = () => {
            console.log('[WebVideoPlayer] onLoadedMetadata');
            onLoad({
                duration: video.duration,
                naturalSize: {
                    width: video.videoWidth,
                    height: video.videoHeight,
                },
            });
        };

        // Handle video end
        const handleEnded = () => {
            console.log('[WebVideoPlayer] onEnded');
            onEnd();
        };

        // Handle errors
        const handleError = (e: Event) => {
            console.log('[WebVideoPlayer] onError', e);
            onError({
                error: {
                    errorString: video.error?.message || 'Unknown video error',
                },
            });
        };

        // Handle waiting (buffering)
        const handleWaiting = () => {
            onBuffer({ isBuffering: true });
        };

        // Handle canplay (buffering ended)
        const handleCanPlay = () => {
            onBuffer({ isBuffering: false });
        };

        // Handle seeked
        const handleSeeked = () => {
            onSeek({ currentTime: video.currentTime });
        };

        video.addEventListener('loadedmetadata', handleLoadedMetadata);
        video.addEventListener('ended', handleEnded);
        video.addEventListener('error', handleError);
        video.addEventListener('waiting', handleWaiting);
        video.addEventListener('canplay', handleCanPlay);
        video.addEventListener('seeked', handleSeeked);

        // Progress reporting
        webProgressIntervalRef.current = setInterval(() => {
            if (video && !video.paused) {
                onProgress({
                    currentTime: video.currentTime,
                    playableDuration: video.buffered.length > 0
                        ? video.buffered.end(video.buffered.length - 1)
                        : video.currentTime,
                });
            }
        }, 500);

        return () => {
            video.removeEventListener('loadedmetadata', handleLoadedMetadata);
            video.removeEventListener('ended', handleEnded);
            video.removeEventListener('error', handleError);
            video.removeEventListener('waiting', handleWaiting);
            video.removeEventListener('canplay', handleCanPlay);
            video.removeEventListener('seeked', handleSeeked);
            if (webProgressIntervalRef.current) {
                clearInterval(webProgressIntervalRef.current);
            }
        };
    }, [streamUrl]); // Only re-run when stream URL changes

    // Handle play/pause state for web
    useEffect(() => {
        if (Platform.OS !== 'web') return;
        const video = webVideoRef?.current;
        if (!video) return;

        if (paused) {
            video.pause();
        } else {
            video.play().catch(err => {
                console.log('[WebVideoPlayer] Play failed:', err);
            });
        }
    }, [paused]);

    // Handle volume for web
    useEffect(() => {
        if (Platform.OS !== 'web') return;
        const video = webVideoRef?.current;
        if (!video) return;
        video.volume = volume;
    }, [volume]);

    // Handle playback rate for web
    useEffect(() => {
        if (Platform.OS !== 'web') return;
        const video = webVideoRef?.current;
        if (!video) return;
        video.playbackRate = playbackSpeed;
    }, [playbackSpeed]);

    // Render based on platform
    if (Platform.OS === 'web') {
        return (
            <View style={{
                position: 'absolute',
                top: 0,
                left: 0,
                right: 0,
                bottom: 0,
                width: '100vw' as any,
                height: '100vh' as any,
                backgroundColor: 'black',
            }}>
                <video
                    ref={webVideoRef}
                    src={streamUrl}
                    style={{
                        position: 'absolute',
                        top: 0,
                        left: 0,
                        width: '100%',
                        height: '100%',
                        objectFit: getWebObjectFit(),
                        backgroundColor: 'black',
                    }}
                    playsInline
                    autoPlay={!paused}
                />

                {/* Gesture overlay for web - simple touch handler */}
                <View style={localStyles.gestureOverlay} pointerEvents="box-only">
                    <TouchableWithoutFeedback onPress={toggleControls}>
                        <View style={localStyles.touchArea} />
                    </TouchableWithoutFeedback>
                </View>
            </View>
        );
    }

    // Native platform render
    return (
        <View style={[styles.videoContainer, {
            width: screenDimensions.width,
            height: screenDimensions.height,
        }]}>
            {useExoPlayer && Video ? (
                /* ExoPlayer via react-native-video */
                <Video
                    ref={exoPlayerRef}
                    source={{
                        uri: streamUrl,
                        headers: headers,
                    }}
                    paused={paused}
                    volume={volume}
                    rate={playbackSpeed}
                    resizeMode={getExoResizeMode()}
                    selectedAudioTrack={selectedAudioTrack}
                    selectedTextTrack={selectedTextTrack}
                    style={localStyles.player}
                    onLoad={handleExoLoad}
                    onProgress={handleExoProgress}
                    onEnd={handleExoEnd}
                    onError={handleExoError}
                    onBuffer={handleExoBuffer}
                    onSeek={handleExoSeek}
                    progressUpdateInterval={500}
                    playInBackground={false}
                    playWhenInactive={false}
                    ignoreSilentSwitch="ignore"
                    automaticallyWaitsToMinimizeStalling={true}
                    useTextureView={true}
                    // Subtitle Styling for ExoPlayer
                    // ExoPlayer supports: fontSize, paddingTop/Bottom/Left/Right, opacity, subtitlesFollowVideo
                    subtitleStyle={{
                        // Convert MPV-scaled size back to ExoPlayer scale (~1.5x conversion was applied)
                        fontSize: subtitleSize ? Math.round(subtitleSize / 1.5) : 18,
                        paddingTop: 0,
                        // Convert MPV position (0=top, 100=bottom) to paddingBottom
                        // Higher MPV position = less padding from bottom
                        paddingBottom: subtitlePosition ? Math.max(20, Math.round((100 - subtitlePosition) * 2)) : 60,
                        paddingLeft: 16,
                        paddingRight: 16,
                        // Opacity controls entire subtitle view visibility
                        // Always keep text visible (opacity 1), background control is limited in ExoPlayer
                        opacity: 1,
                        subtitlesFollowVideo: false,
                    }}
                />
            ) : MpvPlayer ? (
                /* MPV Player fallback */
                <MpvPlayer
                    ref={mpvPlayerRef}
                    source={streamUrl}
                    headers={headers}
                    paused={paused}
                    volume={volume}
                    rate={playbackSpeed}
                    resizeMode={resizeMode === 'none' ? 'contain' : resizeMode}
                    style={localStyles.player}
                    onLoad={handleMpvLoad}
                    onProgress={handleMpvProgress}
                    onEnd={handleMpvEnd}
                    onError={handleMpvError}
                    onTracksChanged={onTracksChanged}
                    decoderMode={decoderMode}
                    gpuMode={gpuMode}
                    // Subtitle Styling
                    subtitleSize={subtitleSize}
                    subtitleColor={subtitleColor}
                    subtitleBackgroundOpacity={subtitleBackgroundOpacity}
                    subtitleBorderSize={subtitleBorderSize}
                    subtitleBorderColor={subtitleBorderColor}
                    subtitleShadowEnabled={subtitleShadowEnabled}
                    subtitlePosition={subtitlePosition}
                    subtitleDelay={subtitleDelay}
                    subtitleAlignment={subtitleAlignment}
                />
            ) : null}

            {/* Gesture overlay - transparent, on top of the player */}
            <PinchGestureHandler
                ref={pinchRef}
                onGestureEvent={onPinchGestureEvent}
                onHandlerStateChange={onPinchHandlerStateChange}
            >
                <View style={localStyles.gestureOverlay} pointerEvents="box-only">
                    <TouchableWithoutFeedback onPress={toggleControls}>
                        <View style={localStyles.touchArea} />
                    </TouchableWithoutFeedback>
                </View>
            </PinchGestureHandler>
        </View>
    );
};

const localStyles = StyleSheet.create({
    player: {
        position: 'absolute',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
    },
    gestureOverlay: {
        position: 'absolute',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
    },
    touchArea: {
        flex: 1,
        backgroundColor: 'transparent',
    },
});

