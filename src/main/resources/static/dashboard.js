const API = "http://localhost:8080/api/tickets";
let selectedTicketId = null;
let currentTicketStatus = "OPEN";

/* ---------- MODAL CONTROL ---------- */
function openModal() {
    selectedTicketId = null;
    currentTicketStatus = "OPEN";
    clearForm();

    document.getElementById("modalTitle").innerText = "Raise an Issue";
    document.getElementById("actionBtn").innerText = "Create Issue";
    document.getElementById("deleteBtn").style.display = "none";

    document.getElementById("modal").style.display = "flex";
}

function openEditModal(id) {
    fetch(`${API}/${id}`)
        .then(res => {
            if (!res.ok) throw new Error(`Failed to fetch ticket: ${res.status}`);
            return res.json();
        })
        .then(t => {
            selectedTicketId = id;
            currentTicketStatus = t.status;

            title.value = t.title;
            description.value = t.description;
            priority.value = t.priority;

            document.getElementById("modalTitle").innerText = "Update Issue";
            document.getElementById("actionBtn").innerText = "Update Issue";
            document.getElementById("deleteBtn").style.display = "inline-block";

            document.getElementById("modal").style.display = "flex";
        })
        .catch(err => {
            console.error("Error loading ticket:", err);
            alert("Error loading ticket details");
        });
}

function closeModal() {
    document.getElementById("modal").style.display = "none";
    selectedTicketId = null;
    currentTicketStatus = "OPEN";
}

function clearForm() {
    title.value = "";
    description.value = "";
    priority.value = "LOW";
    file.value = "";
}

/* ---------- BULK UPLOAD MODAL ---------- */
function openBulkUploadModal() {
    document.getElementById("csvFile").value = "";
    document.getElementById("uploadStatus").innerText = "";
    document.getElementById("bulkUploadModal").style.display = "flex";
}

function closeBulkUploadModal() {
    document.getElementById("bulkUploadModal").style.display = "none";
}

function processBulkUpload() {
    const fileInput = document.getElementById("csvFile");
    const statusDiv = document.getElementById("uploadStatus");
    
    if (!fileInput.files.length) {
        alert("Please select a CSV file");
        return;
    }

    const file = fileInput.files[0];
    
    if (!file.name.endsWith('.csv')) {
        alert("Please select a valid CSV file");
        return;
    }

    statusDiv.innerText = "Uploading CSV file to server...";

    // Create FormData and send to backend
    const formData = new FormData();
    formData.append("file", file);

    fetch(`${API}/bulk-upload`, {
        method: "POST",
        body: formData
    })
    .then(res => {
        if (!res.ok) {
            return res.text().then(text => {
                throw new Error(text || `Upload failed: ${res.status}`);
            });
        }
        return res.json();
    })
    .then(response => {
        console.log("Bulk upload response:", response);
        
        const successCount = response.successCount || 0;
        const failureCount = response.failureCount || 0;
        
        let message = `Bulk upload complete!\n✓ ${successCount} issues created successfully`;
        
        if (failureCount > 0) {
            message += `\n✗ ${failureCount} issues failed`;
            if (response.errors && response.errors.length > 0) {
                console.error("Upload errors:", response.errors);
                message += `\n\nErrors:\n${response.errors.slice(0, 5).join('\n')}`;
                if (response.errors.length > 5) {
                    message += `\n... and ${response.errors.length - 5} more errors (check console)`;
                }
            }
        }
        
        statusDiv.innerText = `✓ Upload complete! ${successCount} created, ${failureCount} failed`;
        
        setTimeout(() => {
            closeBulkUploadModal();
            loadTickets();
            alert(message);
        }, 1500);
    })
    .catch(err => {
        console.error("Bulk upload error:", err);
        statusDiv.innerText = "✗ Upload failed";
        alert("Error during bulk upload: " + err.message);
    });
}



/* ---------- SUBMIT HANDLER ---------- */
function submitTicket() {
    if (selectedTicketId) {
        updateTicket();
    } else {
        createTicket();
    }
}

/* ---------- CREATE ---------- */
function createTicket() {
    if (!title.value || !description.value) {
        alert("Title and Description are required");
        return;
    }

    const formData = new FormData();
    formData.append("title", title.value);
    formData.append("description", description.value);
    formData.append("priority", priority.value);

    if (file.files.length > 0) {
        formData.append("file", file.files[0]);
    }

    fetch(API, {
        method: "POST",
        body: formData
    })
    .then(res => {
        if (!res.ok) throw new Error(`Create failed: ${res.status}`);
        return res.json();
    })
    .then(() => {
        closeModal();
        loadTickets();
        alert("Issue created successfully!");
    })
    .catch(err => {
        console.error("Create error:", err);
        alert("Error creating issue");
    });
}

/* ---------- UPDATE ---------- */
function updateTicket() {
    if (!title.value || !description.value) {
        alert("Title and Description are required");
        return;
    }

    fetch(`${API}/${selectedTicketId}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
            title: title.value,
            description: description.value,
            priority: priority.value,
            status: currentTicketStatus
        })
    })
    .then(res => {
        if (!res.ok) throw new Error(`Update failed: ${res.status}`);
        return res.json();
    })
    .then(() => {
        closeModal();
        loadTickets();
        alert("Issue updated successfully!");
    })
    .catch(err => {
        console.error("Update error:", err);
        alert("Error updating issue");
    });
}

/* ---------- DELETE ---------- */
function deleteSelectedTicket() {
    console.log("=== DELETE DEBUG START ===");
    console.log("Delete function called for ticket ID:", selectedTicketId);
    console.log("Type of selectedTicketId:", typeof selectedTicketId);
    console.log("API base:", API);
    
    if (!selectedTicketId) {
        console.error("ERROR: No ticket selected");
        alert("No ticket selected");
        return;
    }

    if (!confirm("Are you sure you want to delete this issue?")) {
        console.log("Delete cancelled by user");
        return;
    }

    const deleteUrl = `${API}/${selectedTicketId}`;
    console.log("Full DELETE URL:", deleteUrl);

    fetch(deleteUrl, {
        method: "DELETE",
        headers: {
            "Content-Type": "application/json"
        }
    })
    .then(res => {
        console.log("DELETE response received");
        console.log("Response status:", res.status);
        console.log("Response ok:", res.ok);
        console.log("Response statusText:", res.statusText);
        
        return res.text().then(text => {
            console.log("Response body:", text);
            if (!res.ok) {
                throw new Error(`Delete failed with status ${res.status}: ${text}`);
            }
            return text;
        });
    })
    .then((responseText) => {
        console.log("Delete successful!");
        console.log("Response data:", responseText);
        alert("Issue deleted successfully!");
        selectedTicketId = null;
        currentTicketStatus = "OPEN";
        closeModal();
        loadTickets();
        console.log("=== DELETE DEBUG END ===");
    })
    .catch(err => {
        console.error("=== DELETE ERROR ===");
        console.error("Error type:", err.name);
        console.error("Error message:", err.message);
        console.error("Full error:", err);
        console.error("=== DELETE ERROR END ===");
        alert("Error deleting issue: " + err.message);
    });
}

/* ---------- LOAD TICKETS ---------- */
function loadTickets() {
    fetch(API)
        .then(res => {
            if (!res.ok) throw new Error(`Failed to load tickets: ${res.status}`);
            return res.json();
        })
        .then(data => {
            const container = document.getElementById("tickets");
            container.innerHTML = "";

            if (data.length === 0) {
                container.innerHTML = `
                    <div class="empty">
                        🎉 Hurray! No issues yet.
                    </div>`;
                return;
            }

            data.forEach(t => {
                container.innerHTML += `
                    <div class="ticket">
                        <h4>${t.title}</h4>
                        <p>${t.description}</p>
                        <p>
                          <strong>Status:</strong> ${t.status} |
                          <strong>Priority:</strong> ${t.priority}
                        </p>
                        ${t.attachmentPath
                            ? `<a href="${t.attachmentPath}" target="_blank">📎 Attachment</a><br>`
                            : ""}
                        <br>
                        <button class="btn primary"
                                onclick="openEditModal(${t.id})">
                            View / Edit
                        </button>
                    </div>
                `;
            });
        })
        .catch(err => {
            console.error("Load tickets error:", err);
            alert("Error loading tickets");
        });
}

/* ---------- INIT ---------- */
loadTickets();