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
