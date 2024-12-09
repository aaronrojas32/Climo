// Repository details
const repoOwner = "aaronrojas32";
const repoName = "Climo";

// Function to force opening a link in the system's default browser
function openInDefaultBrowser(url) {
  const a = document.createElement("a");
  a.href = url;
  a.target = "_blank"; // Open in a new tab or external browser
  a.rel = "noopener noreferrer"; // Ensure security and avoid issues with WebView
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a); // Clean up the dynamically created link
}

// Function to fetch the latest release and set the download button behavior
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

        // Override the default click behavior to force open in default browser
        downloadBtn.addEventListener("click", function (event) {
          event.preventDefault(); // Prevent default click behavior
          openInDefaultBrowser(apkAsset.browser_download_url); // Force external browser
        });
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