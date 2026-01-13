function initPlayer(injectedUrl, injectedTitle) {
  if (!injectedUrl || injectedUrl.includes("${url}")) {
    injectedUrl = null;
  }

  if (!injectedTitle || injectedTitle.includes("${title}")) {
    injectedTitle = null;
  }

  // Fallback to query param
  function getQueryParam(name) {
    return new URLSearchParams(window.location.search).get(name);
  }
  let streamUrl = injectedUrl || getQueryParam("url");
  let streamTitle = injectedTitle || getQueryParam("title");

  if (streamTitle) {
    const titleDisplay = document.getElementById("movieTitleDisplay");
    if (titleDisplay) titleDisplay.textContent = streamTitle;
  }

  // Display URL for debug if needed, or hide it
  const urlDisplay = document.getElementById("streamUrlDisplay");
  if (urlDisplay && streamUrl) {
    // keeping it hidden as per original class 'd-none' unless logic changes
    urlDisplay.textContent = streamUrl;
  }

  const player = document.getElementById("player");
  const errorDiv = document.getElementById("videoError");

  // Helper to safely extract extension ignoring query params
  function getExtension(url) {
    try {
      const u = new URL(url);
      return u.pathname.split(".").pop().toLowerCase();
    } catch (e) {
      return url.split("?")[0].split(".").pop().toLowerCase();
    }
  }

  const ext = streamUrl ? getExtension(streamUrl) : "";
  let finalUrl = streamUrl;

  // Logic Flow:
  // 1. If TS -> Transcode (Server handles fetching, so mixed content is fine).
  // 2. If Other -> Check Mixed Content -> Proxy if needed.

  if (ext === "ts") {
    console.log("TS file detected. Using transcoder.");
    const transcodeUrl = "/transcode?url=" + encodeURIComponent(streamUrl);
    player.setAttribute("type", "video/mp4");
    player.src = transcodeUrl;

    player.onerror = () =>
      showUnsupported(
        "TS stream could not be played/transcoded. The server might be unable to reach the stream or ffmpeg failed."
      );
  } else {
    // Mixed Content Check
    if (
      streamUrl &&
      window.location.protocol === "https:" &&
      streamUrl.startsWith("http://")
    ) {
      console.log("Mixed content detected. Proxying stream via backend.");
      finalUrl = "/proxy?url=" + encodeURIComponent(streamUrl);
    }

    if (["mkv", "hevc"].includes(ext)) {
      showUnsupported(
        "MKV/HEVC/4K files are not supported in browsers. Use MP4 (H.264/AAC) or HLS (.m3u8)."
      );
    } else if (ext === "m3u8") {
      if (Hls.isSupported()) {
        if (window.hls) window.hls.destroy();
        window.hls = new Hls();
        window.hls.loadSource(finalUrl);
        window.hls.attachMedia(player);
        window.hls.on(Hls.Events.ERROR, (event, data) => {
          if (data.fatal) {
            showUnsupported("HLS playback failed. Please check the stream.");
          }
        });
      } else if (player.canPlayType("application/vnd.apple.mpegurl")) {
        player.src = finalUrl;
      } else {
        showUnsupported("Browser does not support HLS. Use VLC instead.");
      }
    } else {
      // Default (MP4, WebM, etc.)
      let mime = ext === "webm" ? "video/webm" : "video/mp4";
      if (player.canPlayType(mime)) {
        player.setAttribute("type", mime);
        player.src = finalUrl;
      } else {
        showUnsupported("Browser cannot play this file type. Try VLC.");
      }
    }
  }

  // Centralized error handling
  function showUnsupported(msg) {
    if (errorDiv) {
      errorDiv.textContent = msg;
      errorDiv.classList.remove("d-none");
    }
    if (player) player.style.display = "none";
    showVlcButton(streamUrl);
  }

  // VLC helper
  function showVlcButton(url) {
    const vlcContainer = document.getElementById("vlcButtonContainer");
    if (!vlcContainer) return;

    vlcContainer.innerHTML = "";

    // Copy URL button
    const copyBtn = document.createElement("button");
    copyBtn.className = "btn btn-success me-2";
    copyBtn.textContent = "Copy URL for VLC";
    copyBtn.onclick = () => {
      navigator.clipboard.writeText(url).then(() => {
        copyBtn.textContent =
          "Copied! Paste in VLC → Media → Open Network Stream";
        setTimeout(() => {
          copyBtn.textContent = "Copy URL for VLC";
        }, 3000);
      });
    };
    vlcContainer.appendChild(copyBtn);

    // .m3u download link
    const m3uContent = `#EXTM3U\n${url}\n`;
    const m3uBlob = new Blob([m3uContent], { type: "audio/x-mpegurl" });
    const m3uUrl = URL.createObjectURL(m3uBlob);
    const m3uLink = document.createElement("a");
    m3uLink.href = m3uUrl;
    m3uLink.download = "stream.m3u";
    m3uLink.className = "btn btn-primary";
    m3uLink.textContent = "Download .m3u for VLC";
    vlcContainer.appendChild(m3uLink);

    // Help text
    const helpText = document.getElementById("vlcHelpText");
    if (helpText) {
      helpText.innerHTML =
        "If the video does not play in your browser, copy the URL and paste it in VLC: <b>Media → Open Network Stream</b>." +
        "<br>Or download the .m3u file and open it with VLC.";
    }
  }

  // ===== Custom Video Controls =====

  // Seek function
  function seekVideo(seconds) {
    if (!player || !player.duration || isNaN(player.duration)) {
      console.log(
        "Cannot seek: video duration not available (might be a live stream)"
      );
      return;
    }

    const newTime = Math.max(
      0,
      Math.min(player.currentTime + seconds, player.duration)
    );
    player.currentTime = newTime;
  }

  // Format time helper
  function formatTime(seconds) {
    if (!seconds || isNaN(seconds)) return "0:00";

    const hours = Math.floor(seconds / 3600);
    const mins = Math.floor((seconds % 3600) / 60);
    const secs = Math.floor(seconds % 60);

    if (hours > 0) {
      return `${hours}:${mins.toString().padStart(2, "0")}:${secs
        .toString()
        .padStart(2, "0")}`;
    }
    return `${mins}:${secs.toString().padStart(2, "0")}`;
  }

  // Update time display
  function updateTimeDisplay() {
    const currentTimeEl = document.getElementById("currentTime");
    const durationEl = document.getElementById("duration");

    if (currentTimeEl && player) {
      currentTimeEl.textContent = formatTime(player.currentTime);
    }

    if (durationEl && player && player.duration) {
      durationEl.textContent = formatTime(player.duration);
    }
  }

  // Play/Pause toggle
  function togglePlayPause() {
    if (!player) return;

    const icon = document.getElementById("playPauseIcon");
    if (player.paused) {
      player.play();
      if (icon) icon.textContent = "⏸";
    } else {
      player.pause();
      if (icon) icon.textContent = "▶";
    }
  }

  // Button event listeners
  const rewind30Btn = document.getElementById("rewind30");
  const rewind10Btn = document.getElementById("rewind10");
  const playPauseBtn = document.getElementById("playPause");
  const forward10Btn = document.getElementById("forward10");
  const forward30Btn = document.getElementById("forward30");

  if (rewind30Btn) rewind30Btn.addEventListener("click", () => seekVideo(-30));
  if (rewind10Btn) rewind10Btn.addEventListener("click", () => seekVideo(-10));
  if (playPauseBtn) playPauseBtn.addEventListener("click", togglePlayPause);
  if (forward10Btn) forward10Btn.addEventListener("click", () => seekVideo(10));
  if (forward30Btn) forward30Btn.addEventListener("click", () => seekVideo(30));

  // Keyboard shortcuts
  document.addEventListener("keydown", (e) => {
    // Ignore if user is typing in an input field
    if (e.target.tagName === "INPUT" || e.target.tagName === "TEXTAREA") {
      return;
    }

    switch (e.key.toLowerCase()) {
      case "arrowleft":
        e.preventDefault();
        seekVideo(-10);
        break;
      case "arrowright":
        e.preventDefault();
        seekVideo(10);
        break;
      case "j":
        e.preventDefault();
        seekVideo(-10);
        break;
      case "l":
        e.preventDefault();
        seekVideo(10);
        break;
      case "k":
        e.preventDefault();
        togglePlayPause();
        break;
    }
  });

  // Video event listeners for time updates
  if (player) {
    player.addEventListener("timeupdate", updateTimeDisplay);
    player.addEventListener("loadedmetadata", updateTimeDisplay);
    player.addEventListener("durationchange", updateTimeDisplay);

    // Update play/pause icon
    player.addEventListener("play", () => {
      const icon = document.getElementById("playPauseIcon");
      if (icon) icon.textContent = "⏸";
    });

    player.addEventListener("pause", () => {
      const icon = document.getElementById("playPauseIcon");
      if (icon) icon.textContent = "▶";
    });
  }

  // Initial time display update
  updateTimeDisplay();
}
