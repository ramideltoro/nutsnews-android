#!/usr/bin/env swift

import AppKit
import Foundation

private let fileManager = FileManager.default
private let repositoryRoot = URL(fileURLWithPath: fileManager.currentDirectoryPath)
private let outputRoot =
    repositoryRoot.appendingPathComponent(
        "fastlane/metadata/android/en-US/images",
        isDirectory: true
    )

private struct ScreenshotSpec {
    let source: String
    let destination: String
    let maximumWidth: CGFloat
    let maximumHeight: CGFloat
    let cornerRadius: CGFloat
}

private let phoneScreenshots = [
    ScreenshotSpec(
        source: "app/src/test/goldens/phone_s03_s04_s05_s06_feed_populated.png",
        destination: "phoneScreenshots/01-dashboard.png",
        maximumWidth: 932,
        maximumHeight: 1856,
        cornerRadius: 38
    ),
    ScreenshotSpec(
        source: "app/src/test/goldens/phone_s07_s08_s09_s10_s12_article_populated.png",
        destination: "phoneScreenshots/02-story.png",
        maximumWidth: 932,
        maximumHeight: 1856,
        cornerRadius: 38
    ),
    ScreenshotSpec(
        source: "app/src/test/goldens/phone_s15_mood_populated.png",
        destination: "phoneScreenshots/03-good-mood.png",
        maximumWidth: 932,
        maximumHeight: 1856,
        cornerRadius: 38
    ),
    ScreenshotSpec(
        source: "app/src/test/goldens/phone_s17_stats_populated.png",
        destination: "phoneScreenshots/04-reading-stats.png",
        maximumWidth: 932,
        maximumHeight: 1856,
        cornerRadius: 38
    ),
]

private let tabletScreenshots = [
    ScreenshotSpec(
        source: "app/src/test/goldens/tablet_s03_s05_s06_feed_populated.png",
        destination: "tenInchScreenshots/01-dashboard.png",
        maximumWidth: 1016,
        maximumHeight: 1690,
        cornerRadius: 30
    ),
    ScreenshotSpec(
        source: "app/src/test/goldens/tablet_s07_s08_s09_article_populated.png",
        destination: "tenInchScreenshots/02-story.png",
        maximumWidth: 1016,
        maximumHeight: 1690,
        cornerRadius: 30
    ),
    ScreenshotSpec(
        source: "app/src/test/goldens/tablet_s18_settings_populated.png",
        destination: "tenInchScreenshots/03-settings.png",
        maximumWidth: 1016,
        maximumHeight: 1690,
        cornerRadius: 30
    ),
]

private func loadImage(_ relativePath: String) throws -> NSImage {
    let url = repositoryRoot.appendingPathComponent(relativePath)
    guard let image = NSImage(contentsOf: url) else {
        throw NSError(
            domain: "NutsNewsPlayAssets",
            code: 1,
            userInfo: [NSLocalizedDescriptionKey: "Cannot load \(relativePath)"]
        )
    }
    return image
}

private func bitmap(
    width: Int,
    height: Int,
    hasAlpha: Bool,
    draw: () -> Void
) throws -> NSBitmapImageRep {
    let alphaInfo: CGImageAlphaInfo = hasAlpha ? .premultipliedLast : .noneSkipLast
    guard
        let cgContext = CGContext(
            data: nil,
            width: width,
            height: height,
            bitsPerComponent: 8,
            bytesPerRow: width * 4,
            space: CGColorSpaceCreateDeviceRGB(),
            bitmapInfo: alphaInfo.rawValue
        )
    else {
        throw NSError(
            domain: "NutsNewsPlayAssets",
            code: 2,
            userInfo: [NSLocalizedDescriptionKey: "Cannot create bitmap"]
        )
    }

    NSGraphicsContext.saveGraphicsState()
    NSGraphicsContext.current = NSGraphicsContext(cgContext: cgContext, flipped: false)
    draw()
    NSGraphicsContext.current?.flushGraphics()
    NSGraphicsContext.restoreGraphicsState()
    guard let image = cgContext.makeImage() else {
        throw NSError(
            domain: "NutsNewsPlayAssets",
            code: 4,
            userInfo: [NSLocalizedDescriptionKey: "Cannot finalize bitmap"]
        )
    }
    return NSBitmapImageRep(cgImage: image)
}

private func writePNG(_ bitmap: NSBitmapImageRep, relativePath: String) throws {
    let output = outputRoot.appendingPathComponent(relativePath)
    try fileManager.createDirectory(
        at: output.deletingLastPathComponent(),
        withIntermediateDirectories: true
    )
    guard
        let data = bitmap.representation(
            using: .png,
            properties: [.compressionFactor: 1.0]
        )
    else {
        throw NSError(
            domain: "NutsNewsPlayAssets",
            code: 3,
            userInfo: [NSLocalizedDescriptionKey: "Cannot encode \(relativePath)"]
        )
    }
    try data.write(to: output, options: .atomic)
}

private func drawBackground(width: CGFloat, height: CGFloat) {
    let background =
        NSGradient(
            colors: [
                NSColor(calibratedRed: 0.035, green: 0.027, blue: 0.012, alpha: 1),
                NSColor(calibratedRed: 0.105, green: 0.068, blue: 0.012, alpha: 1),
            ]
        )
    background?.draw(
        in: NSRect(x: 0, y: 0, width: width, height: height),
        angle: 90
    )

    NSColor(calibratedRed: 0.98, green: 0.55, blue: 0.02, alpha: 0.12).setFill()
    NSBezierPath(
        ovalIn: NSRect(
            x: width * 0.62,
            y: height * 0.68,
            width: width * 0.58,
            height: width * 0.58
        )
    ).fill()
}

private func renderScreenshot(_ spec: ScreenshotSpec) throws {
    let source = try loadImage(spec.source)
    let canvasWidth: CGFloat = 1080
    let canvasHeight: CGFloat = 1920
    let scale =
        min(
            spec.maximumWidth / source.size.width,
            spec.maximumHeight / source.size.height
        )
    let imageSize =
        NSSize(
            width: floor(source.size.width * scale),
            height: floor(source.size.height * scale)
        )
    let imageRect =
        NSRect(
            x: floor((canvasWidth - imageSize.width) / 2),
            y: floor((canvasHeight - imageSize.height) / 2),
            width: imageSize.width,
            height: imageSize.height
        )

    let rendered =
        try bitmap(width: 1080, height: 1920, hasAlpha: false) {
            drawBackground(width: canvasWidth, height: canvasHeight)

            let shadow = NSShadow()
            shadow.shadowColor = NSColor.black.withAlphaComponent(0.65)
            shadow.shadowBlurRadius = 30
            shadow.shadowOffset = NSSize(width: 0, height: -10)
            shadow.set()

            let clip = NSBezierPath(
                roundedRect: imageRect,
                xRadius: spec.cornerRadius,
                yRadius: spec.cornerRadius
            )
            NSGraphicsContext.saveGraphicsState()
            clip.addClip()
            source.draw(
                in: imageRect,
                from: NSRect(origin: .zero, size: source.size),
                operation: .copy,
                fraction: 1
            )
            NSGraphicsContext.restoreGraphicsState()

            NSColor(calibratedRed: 1, green: 0.65, blue: 0.05, alpha: 0.38).setStroke()
            clip.lineWidth = 3
            clip.stroke()
        }
    try writePNG(rendered, relativePath: spec.destination)
}

private func renderIcon() throws {
    let source = try loadImage("app/src/main/res/drawable-nodpi/brand_icon.png")
    let rendered =
        try bitmap(width: 512, height: 512, hasAlpha: true) {
            NSColor.clear.setFill()
            NSRect(x: 0, y: 0, width: 512, height: 512).fill()
            source.draw(
                in: NSRect(x: 0, y: 0, width: 512, height: 512),
                from: NSRect(origin: .zero, size: source.size),
                operation: .copy,
                fraction: 1
            )
        }
    try writePNG(rendered, relativePath: "icon.png")
}

private func renderFeatureGraphic() throws {
    let source = try loadImage("app/src/main/res/drawable-nodpi/brand_icon.png")
    let rendered =
        try bitmap(width: 1024, height: 500, hasAlpha: false) {
            drawBackground(width: 1024, height: 500)

            let iconRect = NSRect(x: 56, y: 50, width: 400, height: 400)
            NSGraphicsContext.saveGraphicsState()
            NSBezierPath(
                roundedRect: iconRect,
                xRadius: 88,
                yRadius: 88
            ).addClip()
            source.draw(
                in: iconRect,
                from: NSRect(origin: .zero, size: source.size),
                operation: .sourceOver,
                fraction: 1
            )
            NSGraphicsContext.restoreGraphicsState()

            let titleFont =
                NSFont(name: "Avenir Next Heavy", size: 70)
                    ?? NSFont.boldSystemFont(ofSize: 70)
            let subtitleFont =
                NSFont(name: "Avenir Next Demi Bold", size: 27)
                    ?? NSFont.systemFont(ofSize: 27, weight: .semibold)

            ("NutsNews" as NSString).draw(
                at: NSPoint(x: 475, y: 250),
                withAttributes: [
                    .font: titleFont,
                    .foregroundColor: NSColor(calibratedWhite: 0.98, alpha: 1),
                    .kern: 0.8,
                ]
            )
            ("Positive stories. Calmer reading." as NSString).draw(
                at: NSPoint(x: 479, y: 205),
                withAttributes: [
                    .font: subtitleFont,
                    .foregroundColor:
                        NSColor(calibratedRed: 1, green: 0.77, blue: 0.2, alpha: 1),
                ]
            )
        }
    try writePNG(rendered, relativePath: "featureGraphic.png")
}

do {
    try renderIcon()
    try renderFeatureGraphic()
    for screenshot in phoneScreenshots + tabletScreenshots {
        try renderScreenshot(screenshot)
    }
    print("Generated deterministic Google Play assets in \(outputRoot.path)")
} catch {
    fputs("Play asset generation failed: \(error.localizedDescription)\n", stderr)
    exit(1)
}
