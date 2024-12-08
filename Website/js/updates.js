// Agregar el script de Marked.js desde un CDN
const script = document.createElement("script");
script.src = "https://cdn.jsdelivr.net/npm/marked/marked.min.js";
document.head.appendChild(script);

document.addEventListener("DOMContentLoaded", function () {
    const updatesContainer = document.getElementById("updates-container");

    // Tu repositorio
    const repoOwner = "aaronrojas32";
    const repoName = "Climo";

    // Fetch de las releases desde la API de GitHub
    fetch(`https://api.github.com/repos/${repoOwner}/${repoName}/releases`)
        .then((response) => {
            if (!response.ok) {
                throw new Error("Failed to fetch releases");
            }
            return response.json();
        })
        .then((releases) => {
            // Iterar sobre cada release y agregarla al contenedor
            releases.forEach((release) => {
                const releaseCard = document.createElement("div");
                releaseCard.className = "card mb-4 shadow-sm";

                // Convertir el cuerpo en Markdown a HTML con Marked.js
                const releaseBodyHTML = marked.parse(release.body);

                // Construir el contenido de cada release
                releaseCard.innerHTML = `
                    <div class="card-body">
                        <h5 class="card-title">${release.name}</h5>
                        <h6 class="card-subtitle mb-2 text-muted">${new Date(release.published_at).toLocaleDateString()}</h6>
                        <div class="card-text">${releaseBodyHTML}</div>
                        <a href="${release.html_url}" target="_blank" class="btn btn-outline-primary mt-3">View on GitHub</a>
                    </div>
                `;
                updatesContainer.appendChild(releaseCard);
            });
        })
        .catch((error) => {
            console.error("Error fetching releases:", error);
            updatesContainer.innerHTML = "<p class='text-danger'>Failed to load updates. Please try again later.</p>";
        });
});