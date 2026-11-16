(function () {
  var KEY = "priority-jmp-teacher-v1";
  var app = document.getElementById("app");
  var data = window.TEACHER;
  if (!data || !data.courses) {
    app.textContent = "The course file did not load.";
    return;
  }

  var courses = data.courses.slice().sort(function (a, b) {
    if (a.id === "jmp") return -1;
    if (b.id === "jmp") return 1;
    return 0;
  });
  var byCourse = {};
  courses.forEach(function (c) { byCourse[c.id] = c; });

  function load() {
    try {
      var raw = JSON.parse(localStorage.getItem(KEY) || "{}");
      return { answers: raw.answers || {}, last: raw.last || {} };
    } catch (e) {
      return { answers: {}, last: {} };
    }
  }

  var progress = load();

  function save() {
    localStorage.setItem(KEY, JSON.stringify(progress));
  }

  function esc(s) {
    return String(s).replace(/[&<>"']/g, function (c) {
      return { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c];
    });
  }

  function rich(text) {
    return String(text).split(/(\*\*[^*]+\*\*|`[^`]+`|«[^»]+»)/g).map(function (part) {
      if (part.slice(0, 2) === "**" && part.slice(-2) === "**") {
        return "<strong>" + esc(part.slice(2, -2)) + "</strong>";
      }
      if (part.charAt(0) === "`" && part.slice(-1) === "`") {
        return "<code>" + esc(part.slice(1, -1)) + "</code>";
      }
      if (part.charAt(0) === "«" && part.slice(-1) === "»") {
        return '<bdi lang="he" dir="rtl">' + esc(part.slice(1, -1)) + "</bdi>";
      }
      return esc(part);
    }).join("");
  }

  function qKey(course, lesson, qid) {
    return course.id + "/" + lesson.id + "/" + qid;
  }

  function chosen(course, lesson, check) {
    return progress.answers[qKey(course, lesson, check.id)] || "";
  }

  function isCorrect(course, lesson, check) {
    return chosen(course, lesson, check) === check.answer;
  }

  function lessonDone(course, lesson) {
    return lesson.checks.every(function (c) { return isCorrect(course, lesson, c); });
  }

  function lessonScore(course, lesson) {
    var n = 0;
    lesson.checks.forEach(function (c) { if (isCorrect(course, lesson, c)) n += 1; });
    return { n: n, total: lesson.checks.length };
  }

  function courseStats(course) {
    var done = 0;
    var checks = 0;
    var checksDone = 0;
    course.lessons.forEach(function (lesson) {
      if (lessonDone(course, lesson)) done += 1;
      lesson.checks.forEach(function (c) {
        checks += 1;
        if (isCorrect(course, lesson, c)) checksDone += 1;
      });
    });
    return { done: done, total: course.lessons.length, checks: checks, checksDone: checksDone };
  }

  var shown = { courseId: courses[0].id, lessonId: courses[0].lessons[0].id };

  function hashHead() {
    var parts = location.hash.replace(/^#/, "").split("/").filter(Boolean);
    return parts[0] || "";
  }

  function isRouteHash() {
    var head = hashHead();
    return !head || !!byCourse[head];
  }

  function parse() {
    var parts = location.hash.replace(/^#/, "").split("/").filter(Boolean);
    var course = byCourse[parts[0]] || byCourse[shown.courseId] || courses[0];
    var lesson = null;
    var i;
    for (i = 0; i < course.lessons.length; i += 1) {
      if (course.lessons[i].id === parts[1]) lesson = course.lessons[i];
    }
    if (!lesson) {
      var lastId = progress.last[course.id];
      for (i = 0; i < course.lessons.length; i += 1) {
        if (course.lessons[i].id === lastId) lesson = course.lessons[i];
      }
    }
    if (!lesson) lesson = course.lessons[0];
    return { course: course, lesson: lesson };
  }

  function blockHtml(block) {
    if (block.t === "p") return "<p>" + rich(block.text) + "</p>";
    if (block.t === "h") return "<h3>" + rich(block.text) + "</h3>";
    if (block.t === "ul") {
      return "<ul>" + block.items.map(function (item) { return "<li>" + rich(item) + "</li>"; }).join("") + "</ul>";
    }
    if (block.t === "ol") {
      return "<ol class='steps'>" + block.items.map(function (item, idx) {
        return "<li><span class='n'>" + (idx + 1) + "</span><span>" + rich(item) + "</span></li>";
      }).join("") + "</ol>";
    }
    if (block.t === "note") {
      return "<aside class='callout " + esc(block.kind || "note") + "'><strong class='k'>" +
        esc(block.title || "Note") + "</strong>" + rich(block.text) + "</aside>";
    }
    if (block.t === "path") {
      var bits = block.steps.map(function (s) { return "<span class='crumb'>" + rich(s) + "</span>"; }).join("<span class='sep'>→</span>");
      return "<div class='path' aria-label='" + esc(block.label || "Menu path") + "'>" + bits + "</div>";
    }
    if (block.t === "keys") {
      var rows = block.rows.map(function (r) {
        return "<tr><th scope='row'><code>" + esc(r.k) + "</code></th><td>" + rich(r.v) + "</td></tr>";
      }).join("");
      return "<table class='keys'><tbody>" + rows + "</tbody></table>";
    }
    if (block.t === "table") {
      var head = "<tr>" + block.head.map(function (h) { return "<th>" + rich(h) + "</th>"; }).join("") + "</tr>";
      var body = block.rows.map(function (row) {
        return "<tr>" + row.map(function (c) { return "<td>" + rich(c) + "</td>"; }).join("") + "</tr>";
      }).join("");
      return "<table class='grid'><thead>" + head + "</thead><tbody>" + body + "</tbody></table>" +
        (block.caption ? "<p class='caption'>" + rich(block.caption) + "</p>" : "");
    }
    if (block.t === "hist") return histHtml(block);
    if (block.t === "outline") return outlineHtml(block);
    if (block.t === "sim") return "<div data-sim='" + esc(block.id) + "'></div>";
    return "";
  }

  function histHtml(block) {
    var max = 1;
    block.bins.forEach(function (b) { if (b.n > max) max = b.n; });
    var bars = block.bins.map(function (b) {
      var h = Math.max(6, Math.round((b.n / max) * 120));
      return "<div class='bar' style='height:" + h + "px' title='" + esc(b.label) + ": " + b.n + "'><span>" + esc(b.label) + "</span></div>";
    }).join("");
    return "<figure class='hist-wrap'><div class='hist' role='img' aria-label='" + esc(block.caption) + "'>" + bars +
      "</div><figcaption class='caption'>" + rich(block.caption) + "</figcaption></figure>";
  }

  function outlineHtml(block) {
    var rows = block.rows.map(function (r) {
      var cls = (r.indent ? "indent" : "") + (r.head ? " head" : "");
      return "<div class='" + cls + "'>" + rich(r.text) + "</div>";
    }).join("");
    return "<div class='outline'><h4>" + esc(block.title) + "</h4>" + rows + "</div>";
  }

  function questionHtml(course, lesson, check) {
    var choice = chosen(course, lesson, check);
    var right = choice === check.answer;
    var cls = choice ? (right ? "q is-right" : "q is-wrong") : "q";
    var options = check.options.map(function (opt) {
      var checked = choice === opt.id ? " checked" : "";
      var dis = right ? " disabled" : "";
      return "<label class='opt'><input type='radio' name='" + esc(qKey(course, lesson, check.id)) +
        "' value='" + esc(opt.id) + "'" + checked + dis + "> <span>" + rich(opt.text) + "</span></label>";
    }).join("");
    var fb = "";
    if (choice && check.why && check.why[choice]) {
      fb = "<div class='feedback' role='status'>" + (right ? "Yes. " : "Not this one. ") + rich(check.why[choice]) + "</div>";
    }
    return "<fieldset class='" + cls + "' data-qid='" + esc(check.id) + "'><legend>" + rich(check.prompt) +
      "</legend>" + options + fb + "</fieldset>";
  }

  function shell(course, lesson) {
    var stats = courseStats(course);
    var score = lessonScore(course, lesson);
    var idx = course.lessons.indexOf(lesson);
    var prev = idx > 0 ? course.lessons[idx - 1] : null;
    var next = idx < course.lessons.length - 1 ? course.lessons[idx + 1] : null;
    var switcher = courses.map(function (c) {
      var current = c.id === course.id ? " aria-current='page'" : "";
      return "<a href='#/" + c.id + "'" + current + ">" + esc(c.name) + "</a>";
    }).join("");
    var items = course.lessons.map(function (l, i) {
      var sc = lessonScore(course, l);
      var done = sc.n === sc.total;
      var state = done ? "Finished" : (sc.n ? sc.n + " of " + sc.total + " checks" : "Not started");
      var current = l.id === lesson.id ? " aria-current='page'" : "";
      return "<li><a href='#/" + course.id + "/" + l.id + "'" + current + "><span class='num'>" +
        String(i + 1).padStart(2, "0") + "</span><span><span class='lesson-name'>" + esc(l.title) +
        "</span><span class='lesson-state" + (done ? " done" : "") + "' data-lesson-state='" + course.id + "/" + l.id + "'>" +
        state + "</span></span></a></li>";
    }).join("");
    var blocks = lesson.blocks.map(blockHtml).join("");
    var checks = lesson.checks.map(function (c) { return questionHtml(course, lesson, c); }).join("");
    var nav = "<div class='nav-row'>" +
      (prev ? "<a href='#/" + course.id + "/" + prev.id + "'>Previous: " + esc(prev.title) + "</a>" : "<span></span>") +
      (next ? "<a href='#/" + course.id + "/" + next.id + "'>Next: " + esc(next.title) + "</a>" : "<span></span>") +
      "</div>";
    return "<button type='button' class='skip' data-skip>Skip to the lesson</button>" +
      "<header class='top'><div class='brand'><div class='eyebrow'>Study desk</div><h1>" + esc(course.name) +
      "</h1><p>" + esc(course.place) + "</p></div><div class='top-side'><nav class='switch' aria-label='Courses'>" +
      switcher + "</nav><div class='meter' data-stat='meter' aria-live='polite'><strong>" + stats.done + " of " +
      stats.total + "</strong> lessons finished · " + stats.checksDone + " of " + stats.checks + " checks</div></div></header>" +
      "<div class='layout'><aside class='side'><p class='side-kicker'>" + esc(course.place) + "</p><p class='side-blurb'>" +
      esc(course.blurb) + "</p><ol class='lessons'>" + items + "</ol><button type='button' class='reset' data-reset>Clear saved progress</button></aside>" +
      "<main class='main' id='lesson' tabindex='-1'><h2>" + esc(lesson.title) + "</h2><p class='lede'>" + rich(lesson.lede) +
      "</p><div class='prose'>" + blocks + "</div><section class='checks' aria-labelledby='check-h'><h3 id='check-h'>Check</h3>" +
      "<p class='check-count' data-stat='lesson'>" + score.n + " of " + score.total + " checks in this lesson</p>" +
      checks + "</section>" + nav + "<p class='foot-note'>" + (course.id === "priority"
        ? "Progress stays in this browser. There is no login. Menus in a live Priority company are often rearranged; search by the screen name when a path is not where you expect it."
        : "Progress stays in this browser. There is no login. Menus match recent JMP. The modeling type on the column is what decides the analysis.") +
      "</p></main></div>";
  }

  function paintQuestion(field, check, choice) {
    var right = choice === check.answer;
    field.classList.toggle("is-right", right);
    field.classList.toggle("is-wrong", !right);
    var fb = field.querySelector(".feedback");
    if (!fb) {
      fb = document.createElement("div");
      fb.className = "feedback";
      fb.setAttribute("role", "status");
      field.appendChild(fb);
    }
    var why = (check.why && check.why[choice]) || "";
    fb.innerHTML = (right ? "Yes. " : "Not this one. ") + rich(why);
    if (right) {
      field.querySelectorAll("input").forEach(function (input) { input.disabled = true; });
    }
  }

  function paintProgress(course) {
    var stats = courseStats(course);
    var meter = app.querySelector("[data-stat='meter']");
    if (meter) {
      meter.innerHTML = "<strong>" + stats.done + " of " + stats.total + "</strong> lessons finished · " +
        stats.checksDone + " of " + stats.checks + " checks";
    }
    course.lessons.forEach(function (lesson) {
      var node = app.querySelector("[data-lesson-state='" + course.id + "/" + lesson.id + "']");
      if (!node) return;
      var sc = lessonScore(course, lesson);
      var done = sc.n === sc.total;
      node.textContent = done ? "Finished" : (sc.n ? sc.n + " of " + sc.total + " checks" : "Not started");
      node.classList.toggle("done", done);
    });
    var lesson = parse().lesson;
    var score = lessonScore(course, lesson);
    var line = app.querySelector("[data-stat='lesson']");
    if (line) line.textContent = score.n + " of " + score.total + " checks in this lesson";
  }

  function bind(course, lesson) {
    var skip = app.querySelector("[data-skip]");
    skip.addEventListener("click", function () {
      var target = document.getElementById("lesson");
      if (target) target.focus();
    });
    var reset = app.querySelector("[data-reset]");
    reset.addEventListener("click", function () {
      if (window.confirm("Clear every saved check for both courses in this browser?")) {
        progress = { answers: {}, last: {} };
        save();
        render();
      }
    });
    app.querySelector(".checks").addEventListener("change", function (e) {
      var input = e.target;
      if (!input.matches || !input.matches("input[type='radio']")) return;
      var field = input.closest(".q");
      var qid = field.getAttribute("data-qid");
      var check = null;
      lesson.checks.forEach(function (c) { if (c.id === qid) check = c; });
      if (!check) return;
      progress.answers[qKey(course, lesson, qid)] = input.value;
      progress.last[course.id] = lesson.id;
      save();
      paintQuestion(field, check, input.value);
      paintProgress(course);
    });
    mountSims();
  }

  function el(tag, attrs, text) {
    var node = document.createElement(tag);
    if (attrs) Object.keys(attrs).forEach(function (k) {
      if (k === "class") node.className = attrs[k];
      else node.setAttribute(k, attrs[k]);
    });
    if (text != null) node.textContent = text;
    return node;
  }

  function mountSims() {
    app.querySelectorAll("[data-sim]").forEach(function (host) {
      if (host.getAttribute("data-sim") === "order") mountOrder(host);
      if (host.getAttribute("data-sim") === "approve") mountApprove(host);
    });
  }

  function mountOrder(host) {
    host.innerHTML = "";
    var state = { customer: null, openItems: false, part: null, qty: "", saved: false, list: null };
    var customers = [
      { id: "C104", name: "Hadar Foods" },
      { id: "C110", name: "Carmel Print" }
    ];
    var parts = [{ id: "KIT-100", name: "Finished kit", price: "120.00" }];

    var sim = el("div", { class: "sim" });
    sim.appendChild(el("div", { class: "sim-top" }, "Practice form · Sales Orders · not the live Priority window"));
    var body = el("div", { class: "sim-body" });
    body.appendChild(el("p", { class: "sim-label" }, "Upper-level form"));
    var fields = el("div", { class: "fields" });

    function addField(label, node) {
      fields.appendChild(el("label", null, label));
      fields.appendChild(node);
    }

    var custRow = el("div", { class: "field-row" });
    var custInput = el("input", { readonly: "readonly", "aria-label": "Customer number" });
    custInput.value = "";
    var custBtn = el("button", { type: "button", class: "choose", "aria-expanded": "false" }, "Choose");
    custRow.appendChild(custInput);
    custRow.appendChild(custBtn);
    addField("Customer No. ★", custRow);
    var nameInput = el("input", { readonly: "readonly", class: "ro", "aria-label": "Customer name" });
    addField("Customer name", nameInput);
    var dateInput = el("input", { readonly: "readonly", class: "ro" });
    dateInput.value = "";
    addField("Date", dateInput);
    var orderInput = el("input", { readonly: "readonly", class: "ro" });
    addField("Order No.", orderInput);
    var status = el("span", { class: "pill" }, "—");
    addField("Status", status);
    body.appendChild(fields);

    var list = el("div", { class: "listbox", role: "listbox", hidden: "hidden" });
    customers.forEach(function (c) {
      var b = el("button", { type: "button", role: "option" }, c.id + "  " + c.name);
      b.addEventListener("click", function () {
        state.customer = c;
        state.list = null;
        list.hidden = true;
        custBtn.setAttribute("aria-expanded", "false");
        custInput.value = c.id;
        nameInput.value = c.name;
        dateInput.value = "16 Nov 2026";
        orderInput.value = "SO-10088";
        status.textContent = "Draft";
        status.className = "pill";
        paint();
      });
      list.appendChild(b);
    });
    body.appendChild(list);
    custBtn.addEventListener("click", function () {
      var open = list.hidden;
      list.hidden = !open;
      custBtn.setAttribute("aria-expanded", open ? "true" : "false");
    });

    var tabs = el("div", { class: "tabs" });
    var itemsBtn = el("button", { type: "button", class: "ghost", "aria-selected": "false" }, "Order Items");
    tabs.appendChild(itemsBtn);
    body.appendChild(tabs);
    var sub = el("div");
    sub.hidden = true;
    var subFields = el("div", { class: "fields" });
    var partRow = el("div", { class: "field-row" });
    var partInput = el("input", { readonly: "readonly", "aria-label": "Part number" });
    var partBtn = el("button", { type: "button", class: "choose" }, "Choose");
    partRow.appendChild(partInput);
    partRow.appendChild(partBtn);
    subFields.appendChild(el("label", null, "Part Number ★"));
    subFields.appendChild(partRow);
    var priceInput = el("input", { readonly: "readonly", class: "ro" });
    subFields.appendChild(el("label", null, "Unit price"));
    subFields.appendChild(priceInput);
    var qty = el("input", { class: "qty", inputmode: "numeric", "aria-label": "Quantity" });
    subFields.appendChild(el("label", null, "Quantity ★"));
    subFields.appendChild(qty);
    sub.appendChild(subFields);
    var partList = el("div", { class: "listbox", hidden: "hidden" });
    parts.forEach(function (p) {
      var b = el("button", { type: "button" }, p.id + "  " + p.name);
      b.addEventListener("click", function () {
        state.part = p;
        partList.hidden = true;
        partInput.value = p.id;
        priceInput.value = p.price + "  (price list)";
        paint();
      });
      partList.appendChild(b);
    });
    sub.appendChild(partList);
    var leave = el("button", { type: "button", class: "go" }, "Leave the line");
    sub.appendChild(leave);
    body.appendChild(sub);

    var coach = el("p", { class: "coach" });
    var ledger = el("ul", { class: "ledger" });
    body.appendChild(coach);
    body.appendChild(ledger);
    sim.appendChild(body);
    sim.appendChild(el("div", { class: "sim-bot" }, "Company on the bottom bar: Northline"));
    host.appendChild(sim);

    itemsBtn.addEventListener("click", function () {
      if (!state.customer) return;
      state.openItems = true;
      sub.hidden = false;
      itemsBtn.setAttribute("aria-selected", "true");
      paint();
    });
    partBtn.addEventListener("click", function () {
      partList.hidden = !partList.hidden;
    });
    qty.addEventListener("input", function () {
      state.qty = qty.value;
      state.saved = false;
      paint();
    });
    leave.addEventListener("click", function () {
      if (!state.part || !state.qty) return;
      state.saved = true;
      paint();
    });

    function paint() {
      if (!state.customer) {
        coach.textContent = "Open the choose list and pick a customer. The name, the date, and the order number fill in. Status starts as Draft.";
      } else if (!state.openItems) {
        coach.textContent = "The parent row now has data. Open the Order Items sub-level. A draft order does not remove units from the warehouse.";
      } else if (!state.part) {
        coach.textContent = "Choose the part. The unit price comes from the price list. You can see where, because the price source is filled in.";
      } else if (!state.saved) {
        coach.textContent = "Record a quantity, then leave the line. Priority writes the record when you leave it. There is no separate Save to wait for.";
      } else {
        coach.textContent = "The line is saved for everyone in company Northline. Status is still Draft, so the warehouse has not moved.";
      }
      ledger.innerHTML = "";
      if (state.saved) {
        ["Order SO-10088 is Draft for " + state.qty + " of KIT-100.",
          "Warehouse balance of KIT-100: unchanged.",
          "Journal entry: none."].forEach(function (line) {
          ledger.appendChild(el("li", null, line));
        });
      }
      leave.disabled = !(state.part && state.qty);
    }
    paint();
  }

  function mountApprove(host) {
    host.innerHTML = "";
    var toShip = 40;
    var onHand = 25;
    var qty = "";
    var warned = false;
    var final = false;

    var sim = el("div", { class: "sim" });
    sim.appendChild(el("div", { class: "sim-top" }, "Practice sub-level · Customer Shipment – Items"));
    var body = el("div", { class: "sim-body" });
    var table = el("table", { class: "grid ship" });
    table.innerHTML = "<thead><tr><th>Part</th><th>Balance to ship</th><th>Warehouse balance</th><th>Quantity</th><th></th></tr></thead>";
    var tb = el("tbody");
    var tr = el("tr");
    tr.appendChild(el("td", null, "KIT-100"));
    tr.appendChild(el("td", null, String(toShip)));
    var balCell = el("td", null, String(onHand) + " in Main");
    tr.appendChild(balCell);
    var qtyCell = el("td");
    var qtyInput = el("input", { type: "number", min: "0", "aria-label": "Quantity to ship" });
    qtyCell.appendChild(qtyInput);
    tr.appendChild(qtyCell);
    var flagCell = el("td");
    var flag = el("button", { type: "button", class: "choose" }, "אשר");
    flag.setAttribute("lang", "he");
    flagCell.appendChild(flag);
    tr.appendChild(flagCell);
    tb.appendChild(tr);
    table.appendChild(tb);
    body.appendChild(table);
    var warn = el("p", { class: "warn" });
    var finalBtn = el("button", { type: "button", class: "go" }, "Set status to Final");
    var coach = el("p", { class: "coach" });
    body.appendChild(warn);
    body.appendChild(finalBtn);
    body.appendChild(coach);
    sim.appendChild(body);
    host.appendChild(sim);

    function paint() {
      var n = qty === "" ? null : Number(qty);
      warned = n != null && n > onHand;
      warn.textContent = warned ? "Priority warns you: this quantity would drive Main negative. Final stays unavailable until the quantity, the warehouse, or the missing receipt is fixed." : "";
      balCell.textContent = (final ? String(onHand - n) : String(onHand)) + " in Main";
      finalBtn.disabled = final || n == null || n <= 0 || warned;
      flag.disabled = final;
      qtyInput.disabled = final;
      if (final) {
        coach.textContent = "Status is Final. KIT-100 in Main moves from 25 to " + (onHand - n) + ". The order stays open for " + (toShip - n) + ".";
      } else if (n === Math.min(toShip, onHand) && !warned) {
        coach.textContent = "Quantity is the lower of balance to ship (" + toShip + ") and the warehouse balance (" + onHand + "). The balance has not changed yet. Set the parent status to Final for that.";
      } else if (!n) {
        coach.textContent = "Flag אשר. Priority fills Quantity with the lower of the balance to ship and the warehouse balance. Or type a different quantity if you are not shipping that much.";
      } else {
        coach.textContent = "You typed a quantity. The warehouse still shows 25, because the document is not Final.";
      }
    }
    flag.addEventListener("click", function () {
      qty = String(Math.min(toShip, onHand));
      qtyInput.value = qty;
      paint();
    });
    qtyInput.addEventListener("input", function () {
      qty = qtyInput.value;
      paint();
    });
    finalBtn.addEventListener("click", function () {
      var n = Number(qty);
      if (!n || n > onHand) return;
      final = true;
      paint();
    });
    paint();
  }

  function render() {
    var route = parse();
    document.title = route.lesson.title + " · " + route.course.name;
    document.body.dataset.course = route.course.id;
    shown.courseId = route.course.id;
    shown.lessonId = route.lesson.id;
    progress.last[route.course.id] = route.lesson.id;
    save();
    app.innerHTML = shell(route.course, route.lesson);
    bind(route.course, route.lesson);
    document.documentElement.scrollTop = 0;
    document.body.scrollTop = 0;
  }

  window.addEventListener("hashchange", function () {
    if (!isRouteHash()) {
      var restore = "#/" + shown.courseId + "/" + shown.lessonId;
      if (location.hash !== restore) location.hash = restore;
      return;
    }
    render();
  });
  if (!location.hash || !isRouteHash()) location.hash = "#/" + shown.courseId + "/" + shown.lessonId;
  render();
})();
