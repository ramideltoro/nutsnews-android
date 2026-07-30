#!/usr/bin/env swift

import AppKit
import Foundation

guard CommandLine.arguments.count == 3 else {
    FileHandle.standardError.write(
        Data("Usage: generate-brand-assets.swift <approved-icon.png> <res-directory>\n".utf8)
    )
    exit(64)
}

let sourceURL = URL(fileURLWithPath: CommandLine.arguments[1])
let resourcesURL = URL(fileURLWithPath: CommandLine.arguments[2], isDirectory: true)

guard let source = NSImage(contentsOf: sourceURL) else {
    FileHandle.standardError.write(Data("Unable to read \(sourceURL.path)\n".utf8))
    exit(65)
}

let densitySizes = [
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
]

func render(size: Int, circular: Bool) throws -> Data {
    guard let bitmap = NSBitmapImageRep(
        bitmapDataPlanes: nil,
        pixelsWide: size,
        pixelsHigh: size,
        bitsPerSample: 8,
        samplesPerPixel: 4,
        hasAlpha: true,
        isPlanar: false,
        colorSpaceName: .deviceRGB,
        bytesPerRow: 0,
        bitsPerPixel: 0
    ) else {
        throw NSError(domain: "NutsNewsBrandAssets", code: 1)
    }

    NSGraphicsContext.saveGraphicsState()
    defer { NSGraphicsContext.restoreGraphicsState() }

    let graphicsContext = NSGraphicsContext(bitmapImageRep: bitmap)
    NSGraphicsContext.current = graphicsContext
    graphicsContext?.imageInterpolation = .high
    graphicsContext?.cgContext.clear(
        CGRect(x: 0, y: 0, width: size, height: size)
    )

    if circular {
        graphicsContext?.cgContext.addEllipse(
            in: CGRect(x: 0, y: 0, width: size, height: size)
        )
        graphicsContext?.cgContext.clip()
    }

    source.draw(
        in: NSRect(
            x: 0,
            y: 0,
            width: CGFloat(size),
            height: CGFloat(size)
        ),
        from: .zero,
        operation: .sourceOver,
        fraction: 1,
        respectFlipped: true,
        hints: [.interpolation: NSImageInterpolation.high]
    )

    guard let data = bitmap.representation(using: .png, properties: [:]) else {
        throw NSError(domain: "NutsNewsBrandAssets", code: 2)
    }
    return data
}

for (density, size) in densitySizes {
    let outputDirectory = resourcesURL.appendingPathComponent(
        "mipmap-\(density)",
        isDirectory: true
    )
    try FileManager.default.createDirectory(
        at: outputDirectory,
        withIntermediateDirectories: true
    )
    try render(size: size, circular: false).write(
        to: outputDirectory.appendingPathComponent("ic_launcher.png"),
        options: .atomic
    )
    try render(size: size, circular: true).write(
        to: outputDirectory.appendingPathComponent("ic_launcher_round.png"),
        options: .atomic
    )
}

print("Generated full-composition legacy and circular round launcher icons.")
