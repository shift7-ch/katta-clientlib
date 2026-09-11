# Homebrew formula template for the Katta Admin CLI.
# .github/workflows/cli.yml substitutes __VERSION__ and __SHA256_MACOS_ARM64__
# on tag builds and pushes the result to shift7-ch/homebrew-katta as Formula/katta.rb.
class Katta < Formula
  desc "Admin CLI to configure a Katta Server including its S3 storage backend"
  homepage "https://katta.cloud/"
  version "__VERSION__"
  license "AGPL-3.0-or-later"

  # Native image is currently built for Apple Silicon only.
  depends_on arch: :arm64
  depends_on :macos

  url "https://github.com/shift7-ch/katta-clientlib/releases/download/#{version}/katta-macos-arm64.tar.gz"
  sha256 "__SHA256_MACOS_ARM64__"

  def install
    bin.install "katta"
    generate_completions_from_executable(bin/"katta", "completion", shells: [:bash],
                                         shell_parameter_format: :arg)
  end

  test do
    assert_match "Usage:", shell_output("#{bin}/katta --help")
  end
end
