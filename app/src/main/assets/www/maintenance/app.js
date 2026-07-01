const SK = "m_p", AK = "a_id";
const $ = id => document.getElementById(id);
const getP = () => { try { return JSON.parse(localStorage.getItem(SK)) || [] } catch { return [] } };
const saveP = p => localStorage.setItem(SK, JSON.stringify(p));

function mapDOM(c) {
    $("check_auto_update").checked = !!c.autoUpdate;
    $("select_interval").value = c.interval || "24";
    $("input_url").value = c.url || "http://";
    $("check_auth").checked = !!c.useAuth;
    $("input_user").value = c.user || "";
    $("input_pass").value = c.pass || "";
    $("input_subpath").value = c.subpath || "";
    toggleBox(); toggleAuth();
}

function getDOM() {
    return {
        autoUpdate: $("check_auto_update").checked,
        interval: $("select_interval").value,
        url: $("input_url").value,
        useAuth: $("check_auth").checked,
        user: $("input_user").value,
        pass: $("input_pass").value,
        subpath: $("input_subpath").value
    };
}
function toggleBox() {
    const e = $("interval_wrapper");
    if (e) e.classList.toggle("hidden-element", !$("check_auto_update").checked);
}

function toggleAuth() {
    const e = $("auth_fields_wrapper");
    if (e) e.classList.toggle("hidden-element", !$("check_auth").checked);
}

/**
 * Synchronizes profiles state and updates visibility constraints on base labels
 */
function upUI(p, aId) {
    const activeProfile = p.find(x => x.id === aId);
    
    // Instead of rendering internal standard selects, we inject the choice text string right onto an active selector button frame layout trigger container node
    const selectorButtonContainer = $("profile_selector_container");
    if (selectorButtonContainer && activeProfile) {
        selectorButtonContainer.innerText = activeProfile.name;
    }
    
    if ($("btn_delete_profile")) $("btn_delete_profile").disabled = p.length <= 1;
}
async function sendNative() {
    try {
        const c = getDOM();
        const url = `/api/maintenance/save?autoUpdate=${c.autoUpdate}&interval=${c.interval}&url=${encodeURIComponent(c.url)}&useAuth=${c.useAuth}&user=${encodeURIComponent(c.user)}&pass=${encodeURIComponent(c.pass)}&subpath=${encodeURIComponent(c.subpath)}`;
        await fetch(url, { method: "POST" });
    } catch (e) {
        console.error("Save failure:", e.message);
    }
}

function syncActive() {
    const p = getP(), aId = localStorage.getItem(AK), idx = p.findIndex(x => x.id === aId);
    if (idx !== -1) { p[idx].config = getDOM(); saveP(p); }
    sendNative();
}
function handleProfileChange() {
    const id = $("profile_selector").value, p = getP(), x = p.find(i => i.id === id);
    if (x) { localStorage.setItem(AK, id); mapDOM(x.config); sendNative(); }
}

async function createNewConfigProfile() {
    const n = await showModal({ title: "New Profile", isPrompt: true, defVal: "Alternative Setup" });
    if (!n || !n.trim()) return;
    const p = getP(), id = "p_" + Date.now();
    p.push({ id, name: n.trim(), config: getDOM() });
    saveP(p); localStorage.setItem(AK, id);
    upUI(p, id); sendNative();
}

async function renameActiveConfigProfile() {
    const id = localStorage.getItem(AK), p = getP(), idx = p.findIndex(x => x.id === id);
    if (idx === -1) return;
    const n = await showModal({ title: "Rename Profile", isPrompt: true, defVal: p[idx].name });
    if (!n || !n.trim() || n.trim() === p[idx].name) return;
    p[idx].name = n.trim(); saveP(p); upUI(p, id);
}

async function deleteActiveConfigProfile() {
    const p = getP(); 
    if (p.length <= 1) {
        await showModal({ title: "Action Denied", msg: "You cannot delete the sole remaining functional profile track." });
        return;
    }
    const id = localStorage.getItem(AK);
    const conf = await showModal({ title: "Delete Profile", msg: "Are you sure you want to permanently erase this profile context?", isConfirm: true, isDanger: true });
    if (!conf) return;
    const idx = p.findIndex(x => x.id === id); if (idx === -1) return;
    p.splice(idx, 1); saveP(p);
    const fb = p[0].id; localStorage.setItem(AK, fb);
    upUI(p, fb); mapDOM(p[0].config); sendNative();
}
let pollId = null;

async function triggerUpdateDownload() {
    try {
        const btn = $("btn_download"); if (btn) { btn.innerText = "Initializing..."; btn.disabled = true; }
        await fetch("/api/maintenance/download", { method: "POST" });
        if (pollId) clearInterval(pollId);
        pollId = setInterval(async () => {
            try {
                const r = await fetch("/api/maintenance/status"), j = await r.json(), s = j.status;
                if (btn) btn.innerText = s;
                if (s === "Update complete!" || s === "Idle" || s.startsWith("Error")) {
                    clearInterval(pollId);
                    if (btn) { btn.disabled = false; setTimeout(() => btn.innerText = "Download Update", 4e3); }
                }
            } catch { clearInterval(pollId); }
        }, 4e3);
    } catch { const b = $("btn_download"); if (b) { b.innerText = "Download Update"; b.disabled = false; } }
}

async function triggerSDCardSync() {
    try { 
        await fetch("/api/maintenance/sync-sd", { method: "POST" }); 
        await showModal({ title: "Sync Complete", msg: "Workspace assets recursively mirrored to public storage path." });
    } catch (e) { 
        console.error(e); 
    }
}

async function triggerNativeBackClosure() {
    try { await fetch("/api/maintenance/close", { method: "POST" }); } catch (e) { console.error(e); }
}

function updateProgressStatusFromNative(s) { const b = $("btn_download"); if (b) b.innerText = s; }

/**
 * Triggers a flexible custom async overlay to replace blocking browser UI alerts
 */
function showModal({ title, msg, isPrompt, isConfirm, isDanger, defVal }) {
    return new Promise((resolve) => {
        $("modal_title").innerText = title;
        
        const txtEl = $("modal_message");
        if (msg) { txtEl.innerText = msg; txtEl.classList.remove("hidden-element"); } 
        else { txtEl.classList.add("hidden-element"); }
        
        const inpEl = $("modal_input");
        if (isPrompt) { inpEl.value = defVal || ""; inpEl.classList.remove("hidden-element"); setTimeout(() => inpEl.focus(), 50); } 
        else { inpEl.classList.add("hidden-element"); }
        
        const btnBox = $("modal_buttons_container");
        btnBox.innerHTML = "";
        
        if (isPrompt || isConfirm) {
            const btnCancel = document.createElement("button");
            btnCancel.className = "modal-btn-cancel";
            btnCancel.innerText = "Cancel";
            btnCancel.onclick = () => { $("custom_modal").classList.add("hidden-element"); resolve(null); };
            btnBox.appendChild(btnCancel);
        }
        
        const btnOk = document.createElement("button");
        btnOk.className = isDanger ? "modal-btn-danger" : "modal-btn-confirm";
        btnOk.innerText = isConfirm && isDanger ? "Delete" : "OK";
        btnOk.onclick = () => {
            const val = isPrompt ? inpEl.value : true;
            $("custom_modal").classList.add("hidden-element");
            resolve(val);
        };
        
        btnBox.appendChild(btnOk);
        $("custom_modal").classList.remove("hidden-element");
    });
}
/**
 * Triggers visibility closure sequences on the sheet overlay module block
 */
function closeOptionSheet() {
    $("option_sheet_overlay").classList.add("hidden-element");
}

/**
 * Triggers active visibility sequence tracking loops on selection menu elements
 */
function openOptionSheet() {
    const p = getP(), aId = localStorage.getItem(AK);
    const wrap = $("sheet_options_wrapper");
    if (!wrap) return;

    // Dynamically loop and build radio row button structures based on current state array fields
    wrap.innerHTML = p.map(x => `
        <div class="sheet-option-row ${x.id === aId ? 'active-selection' : ''}" onclick="selectProfileFromSheet('${x.id}')">
            <div class="sheet-radio-circle"></div>
            <span class="sheet-option-label">${x.name}</span>
        </div>
    `).join("");

    $("option_sheet_overlay").classList.remove("hidden-element");
}

/**
 * Swaps global settings pointers following an option selection item interaction callback
 */
function selectProfileFromSheet(id) {
    localStorage.setItem(AK, id);
    const p = getP(), act = p.find(x => x.id === id);
    
    if (act) {
        mapDOM(act.config);
        upUI(p, id); // Sync label headers on core view elements
        sendNative();
    }
    closeOptionSheet();
}


document.addEventListener("DOMContentLoaded", async () => {
    let p = getP(), id = localStorage.getItem(AK);
    if (!p.length) {
        let def = { autoUpdate: false, interval: "24", url: "http://", useAuth: false, user: "", pass: "", subpath: "" };
        try { const r = await fetch("/api/maintenance/config"); def = await r.json(); } catch {}
        id = "p_def"; p = [{ id, name: "Default", config: def }]; saveP(p); localStorage.setItem(AK, id);
    }
    let act = p.find(x => x.id === id) || p[0]; id = act.id; localStorage.setItem(AK, id);
    upUI(p, id); mapDOM(act.config); sendNative();
    
    ["check_auto_update", "select_interval", "input_url", "input_subpath", "check_auth", "input_user", "input_pass"].forEach(k => {
        const el = $(k); if (!el) return;
        el.addEventListener(el.type === "checkbox" || el.tagName === "SELECT" ? "change" : "input", () => {
            if (k === "check_auto_update") toggleBox(); if (k === "check_auth") toggleAuth();
            syncActive();
        });
    });
});



