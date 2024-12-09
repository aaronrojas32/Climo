// Repository details
const repoOwner = "aaronrojas32";
const repoName = "Climo";

// Function to check if the user is on a mobile device
function isMobileDevice() {
  return /Mobi|Android/i.test(navigator.userAgent);
}

// Function to fetch the latest release and update the download button
async function fetchLatestRelease() {
  const apiUrl = `https://api.github.com/repos/${repoOwner}/${repoName}/releases/latest`;

  try {
    const response = await fetch(apiUrl);
    if (response.ok) {
      const data = await response.json();

      // Find the APK file in the release assets
      const apkAsset = data.assets.find(asset => asset.name.endsWith(".apk"));
      if (apkAsset) {
        const downloadBtn = document.getElementById("download-apk");
        downloadBtn.href = apkAsset.browser_download_url; // Set the APK link
        downloadBtn.innerHTML = `<i class="bi bi-download"></i> Download APK (${data.name})`;

        // Show a message for mobile users
        if (isMobileDevice()) {
          const message = document.createElement("p");
          message.style.color = "#414e6e";
          message.style.marginTop = "10px";
          message.innerHTML = `
            If the download doesn't start, please copy and paste this link into your browser: 
            <a href="${apkAsset.browser_download_url}" target="_blank" rel="noopener noreferrer">${apkAsset.browser_download_url}</a>
          `;
          downloadBtn.parentNode.appendChild(message);
        }
      } else {
        console.error("No APK file found in the latest release.");
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