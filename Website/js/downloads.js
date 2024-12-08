// Fetch the latest release from GitHub API
const repoOwner = "aaronrojas32";
const repoName = "Climo";

async function fetchLatestRelease() {
  const apiUrl = `https://api.github.com/repos/${repoOwner}/${repoName}/releases/latest`;
  
  try {
    const response = await fetch(apiUrl);
    if (response.ok) {
      const data = await response.json();
      
      // Update the download button with the latest APK link
      const apkAsset = data.assets.find(asset => asset.name.endsWith(".apk"));
      if (apkAsset) {
        const downloadBtn = document.getElementById("download-apk");
        downloadBtn.href = apkAsset.browser_download_url;
        downloadBtn.innerHTML = `<i class="bi bi-download"></i> Download APK (${data.name})`;
      } else {
        console.error("No APK found in the latest release.");
      }
    } else {
      console.error("Failed to fetch the latest release:", response.statusText);
    }
  } catch (error) {
    console.error("Error fetching the latest release:", error);
  }
}

// Run the function on page load
document.addEventListener("DOMContentLoaded", fetchLatestRelease);