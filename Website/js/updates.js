// Add the Marked.js script from a CDN dynamically
const script = document.createElement("script");
script.src = "https://cdn.jsdelivr.net/npm/marked/marked.min.js";
document.head.appendChild(script);

document.addEventListener("DOMContentLoaded", function () {
    const updatesContainer = document.getElementById("updates-container");

    const repoOwner = "aaronrojas32";
    const repoName = "Climo";

    // Fetch the releases from the GitHub API
    fetch(`https://api.github.com/repos/${repoOwner}/${repoName}/releases`)
        .then((response) => {
            if (!response.ok) {
                throw new Error("Failed to fetch releases");
            }
            return response.json(); // Parse the JSON response
        })
        .then((releases) => {
            // Iterate over each release and add it to the container
            releases.forEach((release) => {
                const releaseCard = document.createElement("div");
                releaseCard.className = "card mb-4 shadow-sm";

                // Convert the release body from Markdown to HTML using Marked.js
                const releaseBodyHTML = marked.parse(release.body);

                // Build the content for each release card
                releaseCard.innerHTML = `
                    <div class="card-body">
                        <h5 class="card-title">${release.name}</h5>
                        <h6 class="card-subtitle mb-2 text-muted">${new Date(release.published_at).toLocaleDateString()}</h6>
                        <div class="card-text">${releaseBodyHTML}</div>
                        <a href="${release.html_url}" target="_blank" class="btn btn-outline-primary mt-3">View on GitHub</a>
                    </div>
                `;
                updatesContainer.appendChild(releaseCard); // Append the card to the container
            });
        })
        .catch((error) => {
            // Handle errors during the fetch process
            console.error("Error fetching releases:", error);
            updatesContainer.innerHTML = "<p class='text-danger'>Failed to load updates. Please try again later.</p>";
        });
});