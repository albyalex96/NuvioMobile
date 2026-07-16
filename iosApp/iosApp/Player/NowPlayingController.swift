import Foundation
import MediaPlayer
import UIKit

final class NowPlayingController: NSObject {

    typealias PlayPauseHandler = (Bool) -> Void
    typealias SeekToHandler = (TimeInterval) -> Void
    typealias SeekByHandler = (TimeInterval) -> Void

    var onPlay: (() -> Void)?
    var onPause: (() -> Void)?
    var onSeekTo: SeekToHandler?
    var onSeekForward: (() -> Void)?
    var onSeekBackward: (() -> Void)?
    var onChangePlaybackPosition: SeekToHandler?

    private let nowPlayingInfoCenter = MPNowPlayingInfoCenter.default()
    private let commandCenter = MPRemoteCommandCenter.shared()
    private var registered = false

    func start() {
        guard !registered else { return }
        registered = true

        commandCenter.playCommand.addTarget { [weak self] _ in
            self?.onPlay?()
            return .success
        }

        commandCenter.pauseCommand.addTarget { [weak self] _ in
            self?.onPause?()
            return .success
        }

        commandCenter.togglePlayPauseCommand.addTarget { [weak self] _ in
            if MPNowPlayingInfoCenter.default().nowPlayingInfo?[MPNowPlayingInfoPropertyPlaybackRate] as? Double == 0 {
                self?.onPlay?()
            } else {
                self?.onPause?()
            }
            return .success
        }

        commandCenter.nextTrackCommand.addTarget { [weak self] _ in
            self?.onSeekForward?()
            return .success
        }

        commandCenter.previousTrackCommand.addTarget { [weak self] _ in
            self?.onSeekBackward?()
            return .success
        }

        commandCenter.changePlaybackPositionCommand.addTarget { [weak self] event in
            guard let event = event as? MPChangePlaybackPositionCommandEvent else {
                return .commandFailed
            }
            self?.onChangePlaybackPosition?(event.positionTime)
            return .success
        }
    }

    func stop() {
        guard registered else { return }
        registered = false

        commandCenter.playCommand.removeTarget(self)
        commandCenter.pauseCommand.removeTarget(self)
        commandCenter.togglePlayPauseCommand.removeTarget(self)
        commandCenter.nextTrackCommand.removeTarget(self)
        commandCenter.previousTrackCommand.removeTarget(self)
        commandCenter.changePlaybackPositionCommand.removeTarget(self)

        nowPlayingInfoCenter.nowPlayingInfo = nil
    }

    func updateMetadata(title: String, subtitle: String?, artworkUrl: String?) {
        var info: [String: Any] = [
            MPMediaItemPropertyTitle: title,
            MPNowPlayingInfoPropertyPlaybackProgress: 0.0,
            MPNowPlayingInfoPropertyPlaybackRate: 0.0,
            MPNowPlayingInfoPropertyElapsedPlaybackTime: 0.0,
        ]

        let subtitle = subtitle?.trimmingCharacters(in: .whitespacesAndNewlines)
            .nilIfEmpty

        if let subtitle {
            info[MPMediaItemPropertyArtist] = subtitle
        }

        if let artworkUrl,
           let url = URL(string: artworkUrl) {
            loadArtwork(url: url) { image in
                var updatedInfo = MPNowPlayingInfoCenter.default().nowPlayingInfo ?? info
                if let image {
                    updatedInfo[MPMediaItemPropertyArtwork] = MPMediaItemArtwork(
                        boundsSize: image.size
                    ) { _ in image }
                }
                MPNowPlayingInfoCenter.default().nowPlayingInfo = updatedInfo
            }
        }

        nowPlayingInfoCenter.nowPlayingInfo = info
        start()
    }

    func clearMetadata() {
        stop()
        nowPlayingInfoCenter.nowPlayingInfo = nil
    }

    private func loadArtwork(url: URL, completion: @escaping (UIImage?) -> Void) {
        URLSession.shared.dataTask(with: url) { data, _, error in
            guard let data, error == nil else {
                DispatchQueue.main.async { completion(nil) }
                return
            }
            let image = UIImage(data: data)
            DispatchQueue.main.async { completion(image) }
        }.resume()
    }
}

private extension String {
    var nilIfEmpty: String? {
        isEmpty ? nil : self
    }
}

final class PlayerNowPlayingController {
    private struct Metadata {
        var title: String?
        var subtitle: String?
        var artworkUrl: String?
    }

    private struct PlaybackState {
        var isPlaying: Bool = false
        var positionMs: Int64 = 0
        var durationMs: Int64 = 0
        var playbackSpeed: Float = 1.0
    }

    private struct RemoteCommandTarget {
        let command: MPRemoteCommand
        let token: Any
    }

    private weak var owner: MPVPlayerViewController?
    private var metadata = Metadata()
    private var playbackState = PlaybackState()
    private var currentArtworkImage: UIImage?
    private var currentArtworkURL: String?
    private var artworkTask: URLSessionDataTask?
    private var remoteTargets: [RemoteCommandTarget] = []
    private let artworkCache = NSCache<NSString, UIImage>()

    init(owner: MPVPlayerViewController) {
        self.owner = owner
        configureRemoteCommands()
    }

    deinit {
        invalidate()
    }

    func updateMetadata(
        title: String,
        subtitle: String?,
        artworkUrl: String?
    ) {
        let normalizedTitle = title.trimmingCharacters(in: .whitespacesAndNewlines)
        let normalizedSubtitle = subtitle?.trimmingCharacters(in: .whitespacesAndNewlines)
        let normalizedArtworkUrl = artworkUrl?.trimmingCharacters(in: .whitespacesAndNewlines)

        if metadata.title == normalizedTitle,
           metadata.subtitle == normalizedSubtitle,
           currentArtworkURL == normalizedArtworkUrl {
            applyNowPlayingInfo()
            return
        }

        metadata.title = normalizedTitle
        metadata.subtitle = normalizedSubtitle

        if normalizedArtworkUrl != currentArtworkURL {
            currentArtworkURL = normalizedArtworkUrl
            currentArtworkImage = nil
            artworkTask?.cancel()
            artworkTask = nil

            guard let urlString = normalizedArtworkUrl, !urlString.isEmpty else {
                applyNowPlayingInfo()
                return
            }

            if let cached = artworkCache.object(forKey: urlString as NSString) {
                currentArtworkImage = cached
                applyNowPlayingInfo()
                return
            }

            guard let url = URL(string: urlString) else {
                applyNowPlayingInfo()
                return
            }

            let task = URLSession.shared.dataTask(with: url) { [weak self] data, _, _ in
                guard let self else { return }
                guard let data, let image = UIImage(data: data) else { return }
                self.artworkCache.setObject(image, forKey: urlString as NSString)
                DispatchQueue.main.async { [weak self] in
                    guard let self, self.currentArtworkURL == urlString else { return }
                    self.currentArtworkImage = image
                    self.applyNowPlayingInfo()
                }
            }
            artworkTask = task
            task.resume()
        }

        applyNowPlayingInfo()
    }

    func syncPlayback(
        positionMs: Int64,
        durationMs: Int64,
        isPlaying: Bool,
        playbackSpeed: Float
    ) {
        let nextState = PlaybackState(
            isPlaying: isPlaying,
            positionMs: max(0, positionMs),
            durationMs: max(0, durationMs),
            playbackSpeed: playbackSpeed > 0 ? playbackSpeed : 1.0
        )
        let positionChanged = abs(nextState.positionMs - playbackState.positionMs) >= 1_000
        guard playbackState.isPlaying != nextState.isPlaying ||
            playbackState.durationMs != nextState.durationMs ||
            playbackState.playbackSpeed != nextState.playbackSpeed ||
            positionChanged else {
            return
        }

        playbackState = nextState
        applyNowPlayingInfo()
    }

    func clear() {
        artworkTask?.cancel()
        artworkTask = nil
        currentArtworkImage = nil
        currentArtworkURL = nil
        metadata = Metadata()
        playbackState = PlaybackState()
        DispatchQueue.main.async {
            MPNowPlayingInfoCenter.default().nowPlayingInfo = nil
        }
    }

    func invalidate() {
        clear()
        removeRemoteCommandTargets()
    }

    private func applyNowPlayingInfo() {
        guard let title = metadata.title, !title.isEmpty else { return }

        let buildInfo = {
            var info: [String: Any] = [:]
            info[MPMediaItemPropertyTitle] = title
            info[MPNowPlayingInfoPropertyMediaType] = MPNowPlayingInfoMediaType.video.rawValue
            if let subtitle = self.metadata.subtitle, !subtitle.isEmpty {
                info[MPMediaItemPropertyArtist] = subtitle
            }
            if self.playbackState.durationMs > 0 {
                info[MPMediaItemPropertyPlaybackDuration] = Double(self.playbackState.durationMs) / 1000.0
            }
            info[MPNowPlayingInfoPropertyElapsedPlaybackTime] = Double(self.playbackState.positionMs) / 1000.0
            info[MPNowPlayingInfoPropertyPlaybackRate] = Double(
                self.playbackState.isPlaying ? self.playbackState.playbackSpeed : 0.0
            )
            info[MPNowPlayingInfoPropertyIsLiveStream] = self.playbackState.durationMs <= 0
            if let artwork = self.currentArtworkImage {
                info[MPMediaItemPropertyArtwork] = MPMediaItemArtwork(boundsSize: artwork.size) { _ in artwork }
            }

            MPNowPlayingInfoCenter.default().nowPlayingInfo = info
        }

        if Thread.isMainThread {
            buildInfo()
        } else {
            DispatchQueue.main.async(execute: buildInfo)
        }
    }

    private func configureRemoteCommands() {
        guard remoteTargets.isEmpty else { return }

        let center = MPRemoteCommandCenter.shared()
        center.playCommand.isEnabled = true
        center.pauseCommand.isEnabled = true
        center.togglePlayPauseCommand.isEnabled = true
        center.skipForwardCommand.isEnabled = true
        center.skipBackwardCommand.isEnabled = true
        center.changePlaybackPositionCommand.isEnabled = true
        center.skipForwardCommand.preferredIntervals = [10]
        center.skipBackwardCommand.preferredIntervals = [10]

        remoteTargets.append(
            RemoteCommandTarget(
                command: center.playCommand,
                token: center.playCommand.addTarget { [weak self] _ in
                    DispatchQueue.main.async { self?.owner?.playPlayback() }
                    return .success
                }
            )
        )
        remoteTargets.append(
            RemoteCommandTarget(
                command: center.pauseCommand,
                token: center.pauseCommand.addTarget { [weak self] _ in
                    DispatchQueue.main.async { self?.owner?.pausePlayback() }
                    return .success
                }
            )
        )
        remoteTargets.append(
            RemoteCommandTarget(
                command: center.togglePlayPauseCommand,
                token: center.togglePlayPauseCommand.addTarget { [weak self] _ in
                    DispatchQueue.main.async {
                        guard let owner = self?.owner else { return }
                        if owner.isPlayerPlaying {
                            owner.pausePlayback()
                        } else {
                            owner.playPlayback()
                        }
                    }
                    return .success
                }
            )
        )
        remoteTargets.append(
            RemoteCommandTarget(
                command: center.skipForwardCommand,
                token: center.skipForwardCommand.addTarget { [weak self] event in
                    guard let event = event as? MPSkipIntervalCommandEvent else { return .commandFailed }
                    DispatchQueue.main.async {
                        self?.owner?.seekByMs(Int64(event.interval * 1000.0), exact: true)
                    }
                    return .success
                }
            )
        )
        remoteTargets.append(
            RemoteCommandTarget(
                command: center.skipBackwardCommand,
                token: center.skipBackwardCommand.addTarget { [weak self] event in
                    guard let event = event as? MPSkipIntervalCommandEvent else { return .commandFailed }
                    DispatchQueue.main.async {
                        self?.owner?.seekByMs(-Int64(event.interval * 1000.0), exact: true)
                    }
                    return .success
                }
            )
        )
        remoteTargets.append(
            RemoteCommandTarget(
                command: center.changePlaybackPositionCommand,
                token: center.changePlaybackPositionCommand.addTarget { [weak self] event in
                    guard let event = event as? MPChangePlaybackPositionCommandEvent else { return .commandFailed }
                    DispatchQueue.main.async {
                        self?.owner?.seekToMs(Int64(event.positionTime * 1000.0))
                    }
                    return .success
                }
            )
        )
    }

    private func removeRemoteCommandTargets() {
        guard !remoteTargets.isEmpty else { return }

        remoteTargets.forEach { target in
            target.command.removeTarget(target.token)
        }
        remoteTargets.removeAll(keepingCapacity: false)

        let center = MPRemoteCommandCenter.shared()
        center.playCommand.isEnabled = false
        center.pauseCommand.isEnabled = false
        center.togglePlayPauseCommand.isEnabled = false
        center.skipForwardCommand.isEnabled = false
        center.skipBackwardCommand.isEnabled = false
        center.changePlaybackPositionCommand.isEnabled = false
    }
}
