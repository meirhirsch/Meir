window.TEACHER = {
  courses: [
    {
      id: "priority",
      name: "Priority",
      place: "Second course",
      blurb: "Priority Software, the ERP. The longer course: how the system is organized, how a form behaves, and the documents a user finishes in a day.",
      lessons: [
        {
          id: "organized",
          title: "How Priority is put together",
          lede: "You work inside one company, on forms that share one database. The menu is a map of that company, and it is often redrawn.",
          blocks: [
            { t: "p", text: "Priority, from Priority Software, is the system of record for a company: customers, parts, orders, warehouses, and the books live in one database. You do not keep a private copy. When you leave a line, the record is saved for everyone else in that company." },
            { t: "p", text: "You always work inside one company. Its name sits on the bottom bar of the window. A document that “disappeared” is often open under the other company. Look there before you assume it was deleted." },
            { t: "h", text: "What a menu entry is" },
            { t: "p", text: "The main menu is a tree of modules. Each package contains a different set, and many companies rearrange the tree, hide screens, or rename them. Learn the screen’s name. Find it with the search bar or a home-page tile. A printed path is the standard menu, not a promise that your company left it there." },
            { t: "ul", items: [
              "**Menu** opens another level of the tree.",
              "**Form** is a screen where you retrieve and edit records. Sales Orders, Customers, and the part card are forms.",
              "**Report** reads data and lays it out. It does not change the document.",
              "**Program** does a job: close invoices, prepare documents from orders, run planning.",
              "**Printout** is the document you send or print."
            ] },
            { t: "p", text: "Home-page tiles are your short list. On the menu, right-click an entry and choose Add to Home Page. A magnifying glass on a form tile opens it ready to retrieve records. A plus opens a new record." },
            { t: "p", text: "The search bar, on the home page and at the top of other screens, finds a document by a customer, a vendor, or a number. Type a few letters and pick a line, or press Enter for a results form grouped by document type." },
            { t: "h", text: "The modules this course uses" },
            { t: "table", head: ["Module", "What you do there"], rows: [
              ["Sales", "Quotes and sales orders"],
              ["Inventory", "Parts, warehouses, customer shipments, goods receipts"],
              ["Purchasing", "Purchase demands and purchase orders"],
              ["Financials", "Tax invoices, vendor invoices, receipts, the entry journal"],
              ["Production", "Bills of materials, work orders, the planning run"]
            ], caption: "CRM, service, projects, and human resources use the same form habits. This course stays with the chain from a part card to a document to the journal." },
            { t: "note", kind: "screen", title: "Two clients, one set of documents", text: "There is a Windows client and a web client. The documents are the same. Shortcuts in the next lesson are from Priority’s own user guide for the current interface. Where a site still says «הפעלה ישירה», that is the Actions list: programs and printouts that run on the record already on screen." },
            { t: "note", kind: "example", title: "Worked search", text: "You need yesterday’s order for Hadar. The bottom bar says Northline. In the search bar, type Hadar. The results pane can show a customer, sales orders, and invoices. Choose Sales Orders, then the order. If the list is empty, check the company name before you search again." }
          ],
          checks: [
            {
              id: "company",
              prompt: "You cannot find a shipment you recorded this morning. What do you check first?",
              options: [
                { id: "a", text: "Whether you are in the same company the bottom bar showed when you recorded it" },
                { id: "b", text: "Whether the part card was deleted" },
                { id: "c", text: "Whether planning has been run" }
              ],
              answer: "a",
              why: {
                a: "Documents belong to a company. The bottom bar names the one you are in. A different company is an empty search, not a deleted shipment.",
                b: "Deleting a part card is a rare, blocked act, and it would not hide one shipment by itself. The company comes first.",
                c: "Planning reads documents. It does not file them."
              }
            },
            {
              id: "form",
              prompt: "Which menu entry lets you change a sales order?",
              options: [
                { id: "a", text: "A report" },
                { id: "b", text: "A form" },
                { id: "c", text: "A printout" }
              ],
              answer: "b",
              why: {
                a: "A report reads. It does not edit the order.",
                b: "A form retrieves and updates records. Sales Orders is a form.",
                c: "A printout produces the document you send. The order itself is changed on the form."
              }
            },
            {
              id: "menu",
              prompt: "Customer Shipments is not where the manual says it is. What is the reliable next step?",
              options: [
                { id: "a", text: "Recreate the shipment in a spreadsheet and paste it in later" },
                { id: "b", text: "Search by the screen name, or open it from a home-page tile" },
                { id: "c", text: "Run planning so the menu rebuilds" }
              ],
              answer: "b",
              why: {
                a: "A spreadsheet is a second copy. The shipment has to be the Priority document, or stock and the order will not agree.",
                b: "Menus are customized. The screen name travels better than the path.",
                c: "Planning does not rebuild the menu."
              }
            }
          ]
        },
        {
          id: "forms",
          title: "Working in a form",
          lede: "A form opens ready to retrieve. You choose values from lists, you write lines in a sub-level, and the record saves when you leave the line.",
          blocks: [
            { t: "p", text: "Most of the work is forms. Each record is a row of columns. One column in one record is a field. Mandatory columns are marked with a red star. Read-only columns are gray. If you are stuck on a new line because a mandatory value is missing, undo with Ctrl+Z or clear the line. You cannot leave a half-filled mandatory record behind." },
            { t: "p", text: "The form opens in Query mode, waiting for you to say which records you want. Press Enter to move to data entry. Press F11, or Ctrl+R, to return to Query. In Query mode the active field turns green. Type a value and press Enter to retrieve. An asterisk stands for any string, and a question mark stands for one character. Those patterns are case-sensitive." },
            { t: "keys", rows: [
              { k: "F11", v: "Query mode. The field turns green. Enter runs the query and returns to data entry." },
              { k: "Ctrl+Enter", v: "New record. The plus on a home-page tile does the same." },
              { k: "Ctrl+Z", v: "Undo the last action." },
              { k: "F7", v: "Hide the current line from this retrieval. The record is not deleted." },
              { k: "Ctrl+Delete", v: "Delete the record, and only when its status still allows it." },
              { k: "F4", v: "Make the current single-record or multi-record view the default for this form." },
              { k: "F1", v: "Field help, including how a price was chosen." }
            ] },
            { t: "p", text: "A new record is written when you leave the line: you move to another line, open a sub-level, run a query, or exit the form. There is no separate Save to wait for. A colleague in the same company can already see the line." },
            { t: "h", text: "Choose lists and sub-levels" },
            { t: "p", text: "A choose list sits behind the arrow on a field such as Customer No. or Part Number. Type the start of the name in the pattern box at the top of the list and press Enter. Do not invent a code. If the customer is not in the list, the customer does not exist, or it is blocked, or you are in the wrong company." },
            { t: "p", text: "A sub-level hangs off one parent record. On Sales Orders, Order Items is a sub-level: many lines for one order. Sub-levels appear as tabs under the record. The parent line has to contain data before a sub-level will open. The path bar at the top shows where you are. Click a name in that path to go back up. In Hebrew you will hear «מסך אב» for the parent and «מסך בן» for the sub-level." },
            { t: "p", text: "Multi-record view is a table of many rows. Single-record view groups the columns into tabs such as General, References («אסמכתאות»), and Price. The Actions list on the form runs the programs, reports, and printouts that belong to this record. Closing a tax invoice and printing a shipment are actions. They see the document already on screen. You do not retype its number." },
            { t: "p", text: "The i icon beside Customer, Part Number, Vendor, or Project opens a side panel with the related card, so you can check a hold or a balance without losing the document." },
            { t: "sim", id: "order" },
            { t: "note", kind: "note", title: "What the practice form is showing", text: "Choosing the customer fills the name, the date, and the order number, and the status is Draft. The unit price on the line comes from the price list. Leaving the line saves it. Nothing is removed from the warehouse, and nothing is posted, while the order is Draft." }
          ],
          checks: [
            {
              id: "mode",
              prompt: "You open Sales Orders and the active field is green, with no records showing. What mode is that?",
              options: [
                { id: "a", text: "Query mode. Press Enter for data entry." },
                { id: "b", text: "Final mode. The order is already posted." },
                { id: "c", text: "Sub-level mode. You are already on Order Items." }
              ],
              answer: "a",
              why: {
                a: "A form opens in Query mode, and the active field turns green. Enter moves you to data entry. F11 brings Query back.",
                b: "Final is a document status, not the green query field. A new form has not posted anything.",
                c: "A sub-level is a tab under a parent that already has data. Green means Query."
              }
            },
            {
              id: "choose",
              prompt: "You type a customer number that is not in the choose list, and the form will not take it. What does that mean?",
              options: [
                { id: "a", text: "The field is decorative. Type the name in the price tab instead." },
                { id: "b", text: "Priority only accepts a customer that already exists and that you may use." },
                { id: "c", text: "The order has to be Final before a customer can be chosen." }
              ],
              answer: "b",
              why: {
                a: "The customer number is how the order knows who is billed. Skipping the list leaves the order unanchored.",
                b: "Open Customers and create the card, or check that you are in the right company and that the customer is not blocked.",
                c: "You choose the customer while the order is still Draft. Final comes later, on a shipment or when an invoice is closed."
              }
            },
            {
              id: "save",
              prompt: "You filled Order Items and moved back to the order header. A colleague in the same company can already see the line. Why?",
              options: [
                { id: "a", text: "Leaving the line writes the record. There is no separate Save." },
                { id: "b", text: "Moving to the header sets the order to Final, which publishes it." },
                { id: "c", text: "Priority only shares a line after the nightly planning run." }
              ],
              answer: "a",
              why: {
                a: "The record is written when you leave the line, open a sub-level, run a query, or exit the form.",
                b: "Returning to the header does not finalize the order. Draft still does not move stock.",
                c: "Planning is not the save. The colleague can see the line as soon as you leave it."
              }
            }
          ]
        },
        {
          id: "masters",
          title: "Customers, vendors, and the part card",
          lede: "A document can only choose records that already exist. Three cards carry almost every line you will type.",
          blocks: [
            { t: "p", text: "The customer, the vendor, and the part are master data. Orders, shipments, and invoices are documents. If a choose list will not accept a code, the card is missing, blocked, or in another company. Fix the card. Do not bypass the list." },
            { t: "h", text: "The customer" },
            { t: "p", text: "On Customers («לקוחות») the number, the name, the currency, and the VAT code («קוד מע\"מ») flow onto orders and invoices. Payment terms flow too, unless you override them on that document. A billing customer can differ from the customer who placed the order. The invoice then debits the billing customer. Contacts and sites are sub-levels of the customer." },
            { t: "p", text: "On the customer’s financial definitions, a flag decides whether programs that prepare documents from this customer’s orders produce a tax invoice or a shipping document. Credit is considered on the order unless you clear Consider Credit for that order." },
            { t: "h", text: "The vendor and the part" },
            { t: "p", text: "Vendors («ספקים») mirror this for purchasing: currency, terms, and the vendor’s own invoice number, which you record because it is their document, not yours." },
            { t: "p", text: "The part card («כרטיס פריט», form LOGPART) is keyed by the part number, the «מק\"ט». Description, unit, whether the part is inventory-managed, the default warehouse, prices, and the last purchase price live here. A manufactured part points at a bill of materials («עץ מוצר»). If a part is not inventory-managed, shipments and invoices do not change a warehouse balance for it. They only bill." },
            { t: "p", text: "A price that appears by itself on a line came from a price hierarchy: a price list, a customer agreement, the part’s base price, or the last sale. The line usually has a price-source column. F1 on the price column opens that explanation. Change the price only when this document’s agreement is different. The next order goes back to the list." },
            { t: "p", text: "Warehouses («מחסנים») and bins («איתורים») are master data too. A document proposes a default warehouse. You can change it on the header and, when the goods are elsewhere, on the line." },
            { t: "note", kind: "example", title: "A price you did not type", text: "You add BOLT-M8 to an order. Unit price 1.20 fills in, and the price-source column says the customer price list. You did not type it. Leave it, unless this order was negotiated at another price." }
          ],
          checks: [
            {
              id: "part",
              prompt: "An invoice line will not accept a part number. Which record has to exist first?",
              options: [
                { id: "a", text: "The part card" },
                { id: "b", text: "A finalized shipment for that part" },
                { id: "c", text: "A journal entry for that part" }
              ],
              answer: "a",
              why: {
                a: "The choose list on the part column is the part card. No card, no line.",
                b: "You can invoice without a previous shipment, and you can ship only after the part exists. The card comes first.",
                c: "The journal is a result of finalizing a document, not a prerequisite for the part."
              }
            },
            {
              id: "billto",
              prompt: "Hadar places the order. Galilee Finance pays the bills. Where do you say so?",
              options: [
                { id: "a", text: "Type Galilee’s name over Hadar’s on the printed invoice only" },
                { id: "b", text: "Set the billing customer. The invoice debits that account." },
                { id: "c", text: "Put Galilee in the part card’s warehouse field" }
              ],
              answer: "b",
              why: {
                a: "The printout is not the receivable. The customer account on the invoice is.",
                b: "The billing customer can differ from the ordering customer, on the customer or on the order’s customer-details sub-level.",
                c: "The warehouse says where goods move. It does not say who pays."
              }
            },
            {
              id: "nostock",
              prompt: "A service part is not inventory-managed. You finalize a tax invoice for it. What happens to warehouse balances?",
              options: [
                { id: "a", text: "They fall by the quantity on the line" },
                { id: "b", text: "They do not change. The invoice still posts the customer, the income, and the VAT." },
                { id: "c", text: "They rise, because a service is received into the default warehouse" }
              ],
              answer: "b",
              why: {
                a: "Only an inventory-managed part moves a warehouse balance.",
                b: "The part card says this part is not stock. The books still record the sale.",
                c: "A sale does not receive stock. A service part was never in a warehouse."
              }
            }
          ]
        },
        {
          id: "selling",
          title: "Orders, shipments, and the tax invoice",
          lede: "A draft order is a promise. Stock moves when a shipment is Final, or when a tax invoice that also ships is closed. Do not do both for the same units.",
          blocks: [
            { t: "h", text: "The sales order" },
            { t: "p", text: "Open Sales Orders («הזמנת לקוח», form ORDERS). Status starts as Draft («טיוטא»). Choose the customer. The name, the date, and the order number fill in. On the References tab, record the customer’s own PO number. On the Price tab, check the currency. On Financial Terms, check the tax code. In the Order Items sub-level, enter the part, the quantity, the unit price, and the due date. The Inventory for Part and Part Availability sub-levels tell you whether you can promise that date." },
            { t: "p", text: "A draft order does not take units out of the warehouse. Saving it, and even approving it as a document your company will honor, is not the goods movement." },
            { t: "h", text: "The customer shipment" },
            { t: "p", text: "Customer Shipments («משלוחים ללקוח») live on the standard inventory menu. Search for that screen name if your menu was rearranged." },
            { t: "path", label: "Standard menu", steps: ["Inventory", "Inventory Transactions", "Sales Inventory Transactions", "Customer Shipments"] },
            { t: "p", text: "Choose the customer. The shipping warehouse fills in. Link one order on the References tab, in Order («הזמנה»). To take lines from several orders, use the sub-level that lists order lines and flag the ones you want. The items sub-level then shows what is still open." },
            { t: "p", text: "Two numbers matter on the line: balance to ship («יתרה למשלוח») and warehouse balance («יתרה במחסן»). The warehouse balance is for the warehouse on that line, not the sum of every warehouse. Flag «אשר» and Priority fills Quantity with the lower of those two. Type a quantity only when you are shipping a different amount. A quantity above the warehouse balance warns you that the warehouse would go negative." },
            { t: "p", text: "Return to the parent and set Status to the status your company has marked as final. Out of the box that status is Final («סופית»). That is when the warehouse balance drops. A company can add statuses. Read whether the status is marked final, rather than trusting the label alone. To change a final shipment, run Cancel Document Closure («ביטול סגירת תעודה») from the Actions list. It returns the document to Draft. A few fields can also be changed on the dedicated “changes to a final shipment” screens, and those screens only unlock columns that are not gray." },
            { t: "sim", id: "approve" },
            { t: "h", text: "The tax invoice" },
            { t: "p", text: "Tax Invoices («חשבוניות מס») are under Financials. The new invoice receives a temporary number that starts with T. Link the order the same way you linked a shipment: one order on References, or several through a sub-level. Item lines fill in from the order. Check the price-tab total, the tax code, and the warehouse." },
            { t: "path", label: "Standard menu", steps: ["Financials", "Sales (Financials)", "Sales Invoices", "Tax Invoices"] },
            { t: "p", text: "The invoice does not become final because you typed in a status field. You run the action that closes it. In a Hebrew interface that action is «סגירת חשבונית מס», or the action that closes and prints. Closing assigns the permanent number from the year’s numbering template and writes the journal entry. A temporary invoice can be removed with the delete-temporary-invoice action. A closed invoice is cancelled by a cancellation action, to the original date or to another date. You do not edit the amounts on a closed invoice." },
            { t: "note", kind: "caution", title: "Do not issue the same units twice", text: "If you already finalized a shipment, bill it with a multi-shipment invoice («חשבונית מרכזת») so you do not issue the same units again. Use a tax invoice to both ship and bill only when you did not write a separate shipment. The customer’s financial definitions decide which document a preparation program creates from orders." },
            { t: "note", kind: "example", title: "Northline, this morning", text: "Order SO-10088, customer C104 Hadar, 40 of KIT-100. Main holds 25. On the shipment, «אשר» writes 25. Status Final drops Main by 25. Balance to ship on the order becomes 15, and the order stays open. The bill for those 25 is a multi-shipment invoice against that shipment, not a second document that ships." },
            { t: "p", text: "A receipt («קבלה») later records how the customer paid, in cash, check, transfer, or card, and is matched to the invoices it pays. Closing the receipt writes its own journal entry and clears the receivable." }
          ],
          checks: [
            {
              id: "asher",
              prompt: "Balance to ship is 40. The line’s warehouse holds 25. You flag אשר. What quantity is written?",
              options: [
                { id: "a", text: "40, the whole order" },
                { id: "b", text: "25, the lower of the two balances" },
                { id: "c", text: "0, until the order is deleted" }
              ],
              answer: "b",
              why: {
                a: "Flagging the column does not ignore the warehouse. It takes the lower number.",
                b: "Priority fills Quantity with the lower of balance to ship and warehouse balance. The order stays open for the other 15.",
                c: "The flag is how you accept a quantity. It is not a cancellation."
              }
            },
            {
              id: "when",
              prompt: "When does Main’s balance fall?",
              options: [
                { id: "a", text: "When the sales order is saved" },
                { id: "b", text: "When the shipment’s status is the one marked final" },
                { id: "c", text: "When you open the part card" }
              ],
              answer: "b",
              why: {
                a: "A draft order is a promise. It does not issue stock.",
                b: "The final status on the shipment is the goods movement. A tax invoice that itself ships does the same when it is closed, and you should not do both.",
                c: "The part card describes the part. Opening it changes nothing on hand."
              }
            },
            {
              id: "tee",
              prompt: "The tax invoice still shows a number starting with T. Has the journal been written?",
              options: [
                { id: "a", text: "Yes. The T means it was posted this morning." },
                { id: "b", text: "No. T means temporary. Run the close-invoice action." },
                { id: "c", text: "Only if the customer’s PO number is filled in" }
              ],
              answer: "b",
              why: {
                a: "The T is the temporary number, assigned before closing. Posting happens at close, and the permanent number replaces it.",
                b: "In a Hebrew interface the action is «סגירת חשבונית מס». Run it from the form so this invoice is the one that closes.",
                c: "The customer’s PO is a reference. It does not post the invoice."
              }
            },
            {
              id: "twice",
              prompt: "You finalized a shipment for 25 this morning. Which document bills those units without issuing them again?",
              options: [
                { id: "a", text: "A new tax invoice with אשר flagged for 25" },
                { id: "b", text: "A multi-shipment invoice linked to that shipment" },
                { id: "c", text: "Another sales order for the same 25" }
              ],
              answer: "b",
              why: {
                a: "A tax invoice that ships would reduce inventory again. The units already left on the shipment.",
                b: "«חשבונית מרכזת» bills shipments that are already final.",
                c: "A second order is a new promise. It does not bill the shipment you already closed."
              }
            }
          ]
        },
        {
          id: "buying",
          title: "Purchase orders and vendor invoices",
          lede: "Receive and bill once. Either the vendor invoice receives the goods, or a goods receipt does, and a later invoice only bills.",
          blocks: [
            { t: "p", text: "A purchase order («הזמנת רכש», form PORDERS) can be opened from a vendor price list, a vendor quote, a purchase demand, a blanket order, or a sales order. If you type it by hand and there is no vendor price list, Priority uses the part’s purchase price, and if that is empty, the last purchase-order price. Each line records where its price came from." },
            { t: "h", text: "Two ways to receive" },
            { t: "p", text: "Do not mix them for the same units." },
            { t: "p", text: "**The vendor’s invoice arrives with the goods,** and you are not writing a separate goods receipt. Open Vendor Invoices («חשבוניות ספק», form YINVOICES). Record the vendor, the date on their invoice, and their invoice number. Your own number starts as a temporary T number. The warehouse and bin sit on the References tab. The default comes from the inventory-transaction type for vendor invoices. Link one purchase order in Order, or several in the Orders for Invoice sub-level." },
            { t: "p", text: "In the items sub-level («חשבונית ספק - פירוט»), flag «אשר» to take the open order quantity, or type the quantity that actually arrived. Return to the parent, check the price tab, record payment terms in the sub-level if you need them, and finalize. Warehouse balances increase. The purchase order’s open quantity decreases. A fully received line closes, and a fully received order closes. A journal entry is written. The part card’s last purchase price updates." },
            { t: "p", text: "**The goods arrive before the bill,** or in several deliveries. Record a goods receipt from the vendor («קבלת סחורה מספק»). Inventory increases then. When the bill arrives, record a multi vendor invoice («חשבונית ספק מרכזת») linked to those receipts. That invoice writes the liability and does not receive the goods again." },
            { t: "p", text: "A return to a vendor is a negative quantity, or the return-to-vendor document. A debit you send the vendor («חשבונית לחיוב ספק») asks for credit. A credit invoice («חשבונית זיכוי») records the credit they issued." },
            { t: "note", kind: "example", title: "Eighty of a hundred", text: "PO-441 is 100 of RESIN at 4.00. The truck brings 80, and their invoice is in the packet. No goods receipt was written, so this is the receiving vendor invoice. «אשר» would fill 100, which is wrong today, so you type 80 and finalize. RESIN is up 80. The PO line stays open for 20. Last purchase price on the part card becomes 4.00. The journal credits the vendor for 80, not for the whole order." }
          ],
          checks: [
            {
              id: "eighty",
              prompt: "The open purchase line is 100. The truck brought 80. Why not flag אשר?",
              options: [
                { id: "a", text: "אשר would receive the remaining 100" },
                { id: "b", text: "אשר posts the invoice without touching inventory" },
                { id: "c", text: "אשר is only for customer shipments" }
              ],
              answer: "a",
              why: {
                a: "On a line linked to an order, the flag takes the open quantity. Type 80 so the line stays open for 20.",
                b: "On a receiving vendor invoice, finalizing both receives and posts. The flag is about quantity, not about skipping the warehouse.",
                c: "The same flag is on vendor-invoice lines. It means “take the open quantity,” not “this is a sale.”"
              }
            },
            {
              id: "later",
              prompt: "Goods were received yesterday on a goods receipt. Today’s vendor bill should be which document?",
              options: [
                { id: "a", text: "A second receiving vendor invoice for the same quantity" },
                { id: "b", text: "A multi vendor invoice linked to that receipt" },
                { id: "c", text: "A customer shipment, so the warehouse can be corrected" }
              ],
              answer: "b",
              why: {
                a: "A second receiving invoice would increase the warehouse again.",
                b: "«חשבונית ספק מרכזת» bills receipts already in stock and writes the journal.",
                c: "A customer shipment issues goods to a customer. It is not how you record a vendor’s bill."
              }
            },
            {
              id: "lastprice",
              prompt: "What updates on the part card when that receiving vendor invoice is finalized?",
              options: [
                { id: "a", text: "The customer’s VAT code" },
                { id: "b", text: "The last purchase price" },
                { id: "c", text: "The sales-order due date" }
              ],
              answer: "b",
              why: {
                a: "The VAT code lives on the customer and on the document, not as a result of this receipt.",
                b: "The unit price on the finalized vendor line becomes the last purchase price on the part card.",
                c: "A purchase does not rewrite a sales order’s due date."
              }
            }
          ]
        },
        {
          id: "inventory",
          title: "Warehouses and balances",
          lede: "A balance is a quantity of one part in one warehouse. The part card is not the balance. Draft documents do not change it.",
          blocks: [
            { t: "p", text: "Read «יתרה במחסן» on the document line before you flag «אשר». It is the balance in the warehouse named on that line. The Inventory for Part sub-level shows the other warehouses. Change the warehouse on the line when the stock is elsewhere." },
            { t: "p", text: "The status marked final is the posting. A final shipment decreases the balance by the line quantity. A goods receipt, or a receiving vendor invoice, increases it. A draft does neither. Cancelling the closure returns the document to Draft and reverses the movement. That is why the action exists, and why it is not a casual edit." },
            { t: "p", text: "If the quantity is higher than the balance, Priority warns of a negative balance. Treat the warning as a question: is a receipt still in Draft, are you in the wrong warehouse, or does this company allow negatives? Do not click past it as a habit." },
            { t: "p", text: "A transfer between warehouses is its own inventory document. It is not a sale and not a purchase. The company total stays the same. The quantity moves from one warehouse to the other." },
            { t: "p", text: "A count sets the balance to what you counted. It is a financial event as well as a warehouse event, because the value of inventory changes. Count when you mean to. Do not count your way out of a document you posted to the wrong warehouse." },
            { t: "note", kind: "example", title: "Two buildings", text: "KIT-100 is 25 in Main and 10 in North. The shipment’s warehouse is Main, and balance to ship is 40. «אשר» writes 25. The 10 in North are real, and they are invisible to this line until you change the line’s warehouse or split the shipment. After Final for 25 from Main, Main is 0 and North is still 10." }
          ],
          checks: [
            {
              id: "whichwh",
              prompt: "The line says warehouse balance 0. You can see 10 on the shelf in the other building. What are you looking at?",
              options: [
                { id: "a", text: "The balance of the warehouse named on the line" },
                { id: "b", text: "The company-wide total" },
                { id: "c", text: "The quantity on the part card’s price list" }
              ],
              answer: "a",
              why: {
                a: "«יתרה במחסן» is that line’s warehouse. The other building is another row, visible from Inventory for Part.",
                b: "Priority does not put every warehouse into that one column.",
                c: "The price list is a price, not a quantity on a shelf."
              }
            },
            {
              id: "lunch",
              prompt: "You saved a shipment and went to lunch. Status is still Draft. Has Main decreased?",
              options: [
                { id: "a", text: "Yes, saving is the goods movement" },
                { id: "b", text: "No. Draft does not post the balance." },
                { id: "c", text: "Only if the part is not inventory-managed" }
              ],
              answer: "b",
              why: {
                a: "Saving writes the document. The balance waits for the final status.",
                b: "Until the status marked final, Main is unchanged.",
                c: "A part that is not inventory-managed would not move Main even when final. Draft moves nothing either way."
              }
            },
            {
              id: "wrongwh",
              prompt: "You finalized a receipt into North that belonged in Main. What is the clean correction?",
              options: [
                { id: "a", text: "A count that forces Main up and leaves North as it is" },
                { id: "b", text: "Cancel the closure, then receive into Main" },
                { id: "c", text: "Edit the journal entry and leave both warehouses" }
              ],
              answer: "b",
              why: {
                a: "A count would hide the error in two places: Main forced up, North still holding goods that are not there.",
                b: "Cancel Document Closure reverses the receipt. Receive again into Main. A transfer is the other honest tool if the closure should stand and the goods then move.",
                c: "The journal follows the document. Rewriting the journal and leaving the receipt makes the books and the warehouse disagree."
              }
            }
          ]
        },
        {
          id: "planning",
          title: "Planning, the bill of materials, and work orders",
          lede: "Planning answers what to make or buy, and when it has to arrive. In Priority the run is split, and the names overlap.",
          blocks: [
            { t: "p", text: "In Production, the program people call Run MRP («הרצת MRP») builds the master schedule for finished goods from sales orders, forecasts, and the time it takes to build them. In Purchasing, Purchase Planning («תכנון רכש») explodes the bill of materials, nets what you already have and what is already on order, and recommends purchase demands. If you only run the first, purchasing will not see component demands." },
            { t: "p", text: "A bill of materials («עץ מוצר») is a sub-level of the parent part: each child, and how many the parent needs. Planning multiplies. It does not look at the shop-floor shelf by itself. It reads balances, open purchase orders, and open work orders («פק\"עות») already in Priority. A receipt left in Draft is invisible to it." },
            { t: "p", text: "What you get back is recommended work orders for parts you make, and purchase demands («דרישות רכש») for parts you buy. A demand is not yet a purchase order. Purchasing reviews it and turns approved demands into orders, sometimes after a vendor quote. You can also write a purchase demand by hand for a part you reorder on a schedule." },
            { t: "note", kind: "example", title: "Kits and bolts", text: "Assumptions: lot-for-lot, no safety stock. KIT-100 is made. BOLT-M8 is bought. The sales order is 80 kits due in week 4, and 20 are on hand, so 60 must be made. The build takes a week, so the work order starts in week 3, and the bolts have to be in the warehouse then. The bill says 4 bolts per kit, so gross demand is 240. On hand is 100 and nothing is already on order, so the purchase recommendation is 140. Lead time is two weeks, so the purchase order has to go out in week 1. If those 100 bolts are actually still on a draft receipt, planning still thinks you need 140, and you will buy twice. Finalize the receipt, then plan." }
          ],
          checks: [
            {
              id: "half",
              prompt: "You ran “Run MRP” in Production. Purchasing says they see no demands. What is missing?",
              options: [
                { id: "a", text: "Purchase Planning, which turns the schedule and the bill of materials into purchase demands" },
                { id: "b", text: "A customer receipt" },
                { id: "c", text: "Closing every open sales order first" }
              ],
              answer: "a",
              why: {
                a: "The production run schedules finished goods. «תכנון רכש» is the step that recommends what to buy.",
                b: "A customer receipt records money in. It does not create purchase demands.",
                c: "Open sales orders are a source of demand. Closing them would hide the need."
              }
            },
            {
              id: "bolts",
              prompt: "You will make 60 kits. The bill says 4 bolts per kit. 100 bolts are on hand. Nothing else is open. What is the purchase recommendation?",
              options: [
                { id: "a", text: "240" },
                { id: "b", text: "140" },
                { id: "c", text: "60" }
              ],
              answer: "b",
              why: {
                a: "240 is the gross requirement, before the 100 already on hand.",
                b: "60 × 4 = 240, minus 100 on hand, is 140. That is the net, with the assumptions in the example: no safety stock and no open supply.",
                c: "60 is the number of kits to make, not the number of bolts to buy."
              }
            },
            {
              id: "draftgr",
              prompt: "A goods receipt for bolts is still Draft. Does this planning run treat those bolts as available?",
              options: [
                { id: "a", text: "Yes. A saved receipt is on hand." },
                { id: "b", text: "No. Draft stock is not a balance." },
                { id: "c", text: "Only if the vendor invoice number starts with T" }
              ],
              answer: "b",
              why: {
                a: "Saving the receipt writes the document. The balance, which planning reads, waits for the final status.",
                b: "Finalize the receipt, then plan, or you will recommend a purchase you do not need.",
                c: "The T prefix is a temporary invoice number. It does not put draft bolts on hand."
              }
            }
          ]
        },
        {
          id: "finance",
          title: "What finalizing does to the books",
          lede: "You do not retype an ordinary invoice into the journal. Closing the document writes the entry. If the amount is wrong after that, cancel or credit the document.",
          blocks: [
            { t: "p", text: "A tax invoice, until you run the close action, has a temporary number starting with T and has not written its journal entry. Closing gives it the permanent number and posts. For an inventory-managed part, a tax invoice that is also the shipping document relieves inventory as well. A multi-shipment invoice bills shipments you already finalized and does not relieve those units again." },
            { t: "p", text: "The income account comes from the part, unless the invoice line names a different one. VAT uses the tax code on the document, which defaulted from the customer. You do not type the VAT amount by hand. The code carries the rate. In the numbers below, that rate is 18 percent. The balance date («תאריך מאזן») defaults to the invoice date. Change it on the financial-terms tab when the posting period should differ." },
            { t: "p", text: "A receiving vendor invoice posts the vendor, the input VAT, and the inventory or expense, and raises the warehouse balance. A multi vendor invoice posts the vendor against receipts already in stock." },
            { t: "p", text: "A customer receipt, once approved and closed, posts the cash or the bank and clears the receivable the invoice created." },
            { t: "p", text: "You can move from the document to its journal entry («תנועת יומן», the entry journal, form FNCTRANS) with the actions and sub-levels on the invoice. The chart of accounts (ACCOUNTS) is the list of accounts those lines use. If the total is wrong after closing, cancel the document with its cancellation action, or issue a credit. Do not edit the journal by hand and leave the invoice as it is." },
            { t: "note", kind: "example", title: "Two entries, illustrative accounts", text: "These accounts are an illustration for Northline, not a universal chart. A tax invoice that also ships, for 25 of KIT-100 at 120, with the 18 percent tax code and no earlier shipment: customer debit 3,540, income credit 3,000, output VAT credit 540, and KIT-100 down 25. The amount of inventory relieved is the part’s cost. This example does not invent that cost. Read the entry if you need the figure. Separately, the receiving vendor invoice for 80 of RESIN at 4.00: vendor credit 377.60, inventory or purchase debit 320, input VAT debit 57.60, and RESIN on hand up 80." },
            { t: "h", text: "A morning, in order" },
            { t: "ol", items: [
              "Search Hadar and open SO-10088. Order Items shows 40 of KIT-100 still to ship. You do not “send it” by editing the order.",
              "Customer Shipments, customer C104, link SO-10088. The line’s warehouse is Main: balance to ship 40, warehouse balance 25. Flag «אשר». Quantity becomes 25. Set the final status. Main drops by 25. The order stays open for 15. North’s quantity, if any, is not this column.",
              "The vendor’s truck brings 80 of the 100 RESIN on PO-441, with their invoice, and nobody wrote a goods receipt. Vendor Invoices, link the PO, type 80 rather than flagging the full remainder, and finalize. RESIN is up 80. The PO stays open for 20.",
              "Accounting asks you to bill Hadar for what left. You do not open a fresh tax invoice and ship 25 again. You prepare the multi-shipment invoice against the shipment, check the price tab, and run the close action. The number loses its T. Inventory does not move again.",
              "Before anyone trusts a planning recommendation, the RESIN receipt has to be final. A draft receipt is not on hand."
            ] }
          ],
          checks: [
            {
              id: "posted",
              prompt: "The invoice number is T260014. Has it posted?",
              options: [
                { id: "a", text: "No. Close it. The permanent number replaces the T." },
                { id: "b", text: "Yes. Any number on the form is a posted number." },
                { id: "c", text: "Only the warehouse balance can tell you" }
              ],
              answer: "a",
              why: {
                a: "T is temporary. The journal is written by the close action, which also assigns the year’s permanent number.",
                b: "The temporary number is a number. It is not the posting.",
                c: "A service invoice posts with no warehouse movement at all. The number, and the journal entry, are the check."
              }
            },
            {
              id: "debit",
              prompt: "25 units at 120, tax code 18 percent, no earlier shipment. What is the customer debit on that illustrative invoice?",
              options: [
                { id: "a", text: "3,000" },
                { id: "b", text: "3,540" },
                { id: "c", text: "540" }
              ],
              answer: "b",
              why: {
                a: "3,000 is the income, before VAT. The customer owes the tax as well.",
                b: "25 × 120 is 3,000. Eighteen percent is 540. The customer debit is 3,540. You do not type it. The tax code does.",
                c: "540 is the VAT line, not the receivable."
              }
            },
            {
              id: "again",
              prompt: "At 09:05, why is a new shipping tax invoice the wrong tool for the 25 that already left?",
              options: [
                { id: "a", text: "The shipment already issued them. A tax invoice that ships would issue them again." },
                { id: "b", text: "Tax invoices cannot be linked to customers" },
                { id: "c", text: "Main still shows 25, so nothing has shipped" }
              ],
              answer: "a",
              why: {
                a: "Bill with the multi-shipment invoice against the final shipment. Inventory stays where the shipment put it.",
                b: "The tax invoice starts by choosing the customer. Linking is the normal path.",
                c: "After Final, Main has already fallen by 25. Seeing 25 would mean the shipment is not final."
              }
            },
            {
              id: "fix",
              prompt: "The price-tab total is wrong, and the invoice is already closed. What do you do?",
              options: [
                { id: "a", text: "Rewrite the journal entry and leave the invoice unchanged" },
                { id: "b", text: "Cancel the document with its cancellation action, or issue a credit" },
                { id: "c", text: "Flag אשר again on the closed lines" }
              ],
              answer: "b",
              why: {
                a: "The journal was born from the invoice. Editing one and not the other leaves them in disagreement.",
                b: "Use the cancellation action, to the original date or to another date, or a credit invoice, depending on the case.",
                c: "«אשר» fills an open quantity. It is not an edit tool on a closed invoice."
              }
            }
          ]
        }
      ]
    },
    {
      id: "jmp",
      name: "JMP",
      place: "First course",
      blurb: "SAS JMP, following Statistical Thinking for Industrial Problem Solving: eight modules, from how to frame a problem through graphs, quality, tests, regression, experiments, and prediction.",
      lessons: [
        {
          id: "overview",
          title: "Module 0: What this course is for",
          lede: "Eight modules, one habit: a process varies, that variation is information, and JMP is where you look at it.",
          blocks: [
            { t: "p", text: "This course follows Statistical Thinking for Industrial Problem Solving, in the order of its modules. Module 0 is the map. The later modules are the work: frame the problem, explore the data, separate signal from noise, decide with intervals and tests, fit relationships, design an experiment, then predict." },
            { t: "h", text: "Why the foundation comes first" },
            { t: "p", text: "Every process wanders. A yield, a thickness, a scrap rate, a complaint. If you only react to the last bad batch, you chase noise. Statistical thinking treats that wander as something you can measure, picture, and learn from. The point of the course is not a catalog of tests. It is a way to close the gap between how a process performs and how it needs to perform." },
            { t: "h", text: "JMP starts as a table" },
            { t: "p", text: "The getting-started video puts you in a data table. Rows are the things you measured: batches, parts, runs. Columns are the measurements and the labels. JMP decides which analysis fits from the modeling type on the column, so set that before you trust a mean." },
            { t: "ul", items: [
              "**Continuous**: a measurement, such as yield or temperature. The icon is a blue triangle.",
              "**Nominal**: a name, such as supplier, shift, or lot. Red bars. An id made of digits is still a name.",
              "**Ordinal**: an order you mean, such as low, medium, high. Green bars."
            ] },
            { t: "p", text: "A missing number shows as a dot. It is not a zero. A zero was measured. A dot was not. Later analyses tell you how many rows they set aside. Read that count before you trust the result." },
            { t: "note", kind: "screen", title: "How you will work", text: "File, then Open, brings in a JMP table, a spreadsheet, or a text file. The columns panel is on the left. Click a column name to open Column Info and set the modeling type. The icon in the panel should match what the column really is. Save the table. The later modules reuse the same habit: one table, columns typed, then a platform from the Analyze or Graph menu." }
          ],
          checks: [
            {
              id: "gap",
              prompt: "What is this course actually training you to do?",
              options: [
                { id: "a", text: "Close a gap in process performance by learning from variation" },
                { id: "b", text: "Memorize a formula for every test JMP offers" },
                { id: "c", text: "Replace the process with a spreadsheet average" }
              ],
              answer: "a",
              why: {
                a: "The overview is a map of problem solving. Variation is the information you use to close the gap.",
                b: "Formulas show up later, inside a decision. They are not the point of module 0.",
                c: "An average in a sheet is a private copy. The course keeps the measurements in a table and asks what the process is doing."
              }
            },
            {
              id: "idcol",
              prompt: "Lot numbers are 4401, 4402, 4403. Which modeling type?",
              options: [
                { id: "a", text: "Continuous, because they are numbers" },
                { id: "b", text: "Nominal. They are labels." },
                { id: "c", text: "Ordinal, because the numbers go up" }
              ],
              answer: "b",
              why: {
                a: "JMP would then average lot numbers. That average is not a quantity of anything you make.",
                b: "A code is a name, even when it is written with digits.",
                c: "Ordinal is for an order you mean, such as small then large. A lot sequence is not a scale of size."
              }
            },
            {
              id: "dot",
              prompt: "Yield shows a dot in one row. What is that row?",
              options: [
                { id: "a", text: "A measured yield of zero" },
                { id: "b", text: "Missing. It stays out of the mean." },
                { id: "c", text: "The highest yield, flagged so you notice it" }
              ],
              answer: "b",
              why: {
                a: "Zero would pull the mean down and pretend you measured nothing.",
                b: "A dot was not measured. The analysis reports how many such rows it excluded.",
                c: "JMP does not fill a missing cell with the maximum."
              }
            }
          ]
        },
        {
          id: "thinking",
          title: "Module 1: Frame the problem",
          lede: "Name the gap, map the process, list possible causes, and only then decide what to measure.",
          blocks: [
            { t: "p", text: "A problem, in this course, is a failure to meet a level of performance you need. There is a gap. Easy problems have a known cause and a known fix. Hard ones do not. The module walks the same four groups of work either way: define the gap, understand the process, find likely causes, then act and hold the gain." },
            { t: "h", text: "Say the problem so a team can use it" },
            { t: "p", text: "Write what is wrong, where, and how far from the goal. Low yield on black anodize is a gap. An unhappy meeting is not. Put a goal next to it, and a measure that tells you whether you moved. That measure is a key indicator: one number the team agrees to watch." },
            { t: "h", text: "Map the process before you blame a step" },
            { t: "p", text: "A process takes inputs, adds work, and hands an output to a customer. A SIPOC is the wide view: suppliers, inputs, the process in a few steps, outputs, customers. An input-output map is tighter. It shows which inputs feed which steps. Use a flowchart when the path branches or the same work is done by more than one role. The map is a picture of how the work actually happens, not the poster in the hallway." },
            { t: "h", text: "Causes are a list, not a verdict" },
            { t: "ul", items: [
              "**Brainstorm**, then **multi-vote** to shorten the list, or an **affinity** diagram to group the ideas.",
              "A **cause-and-effect diagram** puts the problem at the head and branches the causes. Asking why, about five times, walks down one branch.",
              "A **cause-and-effect matrix** scores those causes against more than one effect, when yield is not the only thing that matters."
            ] },
            { t: "p", text: "None of these tools prove a cause. They stop the team from testing the first idea that was loudest." },
            { t: "h", text: "Define the measurement before you collect it" },
            { t: "p", text: "Continuous, nominal, and ordinal data do not answer the same question. An operational definition says how the number is taken: which gauge, which spot on the part, what counts as a defect. Then choose a collection plan that matches the decision. When the sheet exists, import it into a JMP table and set the modeling types before any platform." },
            { t: "note", kind: "example", title: "A yield gap", text: "White polymer yield is under the goal. The problem statement names the product, the line, and the gap. A SIPOC shows resin, the reactor, and the customer who receives the lot. The fishbone lists temperature, feed rate, and the assay. You do not change the reactor yet. You define how yield is calculated, then you collect." }
          ],
          checks: [
            {
              id: "problem",
              prompt: "Which of these is a problem statement this module would accept?",
              options: [
                { id: "a", text: "Black anodize yield is 82 percent, and the goal is 95 percent" },
                { id: "b", text: "The process should be better" },
                { id: "c", text: "We think the acid bath is the cause" }
              ],
              answer: "a",
              why: {
                a: "It names the gap between current performance and the level you need.",
                b: "Better is not a level. The team cannot tell when the gap has closed.",
                c: "A suspected cause comes after the gap is defined. It is not the problem statement."
              }
            },
            {
              id: "sipoc",
              prompt: "What is a SIPOC for?",
              options: [
                { id: "a", text: "A wide map of suppliers, inputs, steps, outputs, and customers" },
                { id: "b", text: "A proof of the root cause" },
                { id: "c", text: "A control chart of the last twenty lots" }
              ],
              answer: "a",
              why: {
                a: "SIPOC is the wide process picture, before you draw a tighter input-output map.",
                b: "A map does not test a cause. It shows where a cause could sit.",
                c: "Control charts are a later module. They need data you have not defined yet."
              }
            },
            {
              id: "fish",
              prompt: "You finish a fishbone and five whys. What do you have?",
              options: [
                { id: "a", text: "A ranked list of possible causes, still untested" },
                { id: "b", text: "The root cause, ready to change on the line" },
                { id: "c", text: "A confidence interval for the yield" }
              ],
              answer: "a",
              why: {
                a: "These are tools for generating and sorting ideas. Data still has to test them.",
                b: "Changing the line on a diagram alone is how you fix the wrong thing.",
                c: "An interval needs measurements. This step has not collected them."
              }
            }
          ]
        },
        {
          id: "explore",
          title: "Module 2: Look before you test",
          lede: "Describe each column, picture relationships, and clean the table before a p-value means anything.",
          blocks: [
            { t: "p", text: "Exploratory data analysis is numerical summaries and pictures, used to learn what the sample actually looks like. You are not confirming a theory yet. You are seeing shape, center, spread, odd points, and which columns move together." },
            { t: "h", text: "One continuous column" },
            { t: "p", text: "Analyze, then Distribution. Put the measurement in Y. The histogram and the box plot come up together, with the mean, the median, and the spread. Variance and standard deviation describe spread around the mean. The range and the interquartile range describe spread without squaring anything." },
            { t: "p", text: "Columns Viewer and Tabulate summarize many continuous columns, or a continuous column split by a category such as reactor. Hide and exclude are different: hide removes a point from a graph, exclude removes it from the calculations. Do both when a point is a typing error. Do neither just because the point is inconvenient." },
            { t: "h", text: "Which graph, and how you open it" },
            { t: "p", text: "Pick the graph from the question, then open that one. Graph Builder is Graph, then Graph Builder. Drag a column into a zone, then click the icon for the element you want: points, bars, a line, a tree map, a heat map." },
            { t: "h", text: "Histogram" },
            { t: "p", text: "Use it for the shape of one continuous column: one peak, two peaks, or a long tail. Open Analyze, then Distribution, and put the column in Y. Choose Histograms Only when you want the bars without the other summaries." },
            { t: "h", text: "Box plot" },
            { t: "p", text: "Use it for the center, the middle half, and points that sit far out. One column: the box plot is already in Distribution. Several groups, such as reactors: Graph Builder, the measurement on Y, the group on X, then the box plot icon." },
            { t: "h", text: "Normal quantile plot" },
            { t: "p", text: "Use it to judge whether a continuous column is roughly bell-shaped. Points along a line are close to normal. A curve means a tail. Open Distribution, then the red triangle beside that column, and choose the normal quantile plot. Continuous Fit, then Normal, draws the curve on the histogram." },
            { t: "h", text: "Scatterplot" },
            { t: "p", text: "Use it when both columns are continuous and you need to see a line, a curve, a fan shape, or a clump. Graph Builder: one column on Y, the other on X. A smoother is optional. Do not quote a correlation until you have seen this picture." },
            { t: "h", text: "Scatterplot matrix" },
            { t: "p", text: "Use it when you have several continuous columns and you want every pair at once, to see which pairs are worth a closer look. Open Graph, then Scatterplot Matrix, and put those columns in Y." },
            { t: "h", text: "Run chart" },
            { t: "p", text: "Use it when the rows are in time order, or batch order, and you want to know whether the measurement drifted, jumped, or cycled. A histogram throws that order away. Graph Builder: time or batch on X, the measurement on Y, then the line icon." },
            { t: "h", text: "Bar chart" },
            { t: "p", text: "Use it for how often each category occurs, or for a summary such as a mean in each category. Graph Builder: the category on an axis, then the bar icon. A nominal column in Distribution also draws bars with counts. Sort the bars when you care which categories are largest." },
            { t: "h", text: "Pareto plot" },
            { t: "p", text: "Use it when many categories are problems and you want the few that account for most of them, largest on the left. Graph Builder: the category on X, the count on the bar length, the bar icon, then order the bars from largest to smallest." },
            { t: "h", text: "Mosaic plot" },
            { t: "p", text: "Use it for two categories together: whether fail and pass sit in the same pattern inside each reactor, for example. Analyze, then Fit Y by X. Put one nominal column in Y and the other in X. The widths and heights are the counts." },
            { t: "h", text: "Packed bar chart" },
            { t: "p", text: "Use it for one category with so many levels that an ordinary bar chart becomes a long scroll, such as dozens of company names. Graph Builder: the category on Y, the bar icon, then the packed bar element so the bars fill a block instead of a list." },
            { t: "h", text: "Tree map" },
            { t: "p", text: "Use it when many categories should be compared by size, and the biggest ones should dominate the picture. Graph Builder: the category on X, the tree map icon, and a continuous column to size the rectangles." },
            { t: "h", text: "Trellis" },
            { t: "p", text: "Use it when you already have a graph and you want that same graph repeated once per group, instead of crowding every group onto one set of axes. Build the graph in Graph Builder, then put the grouping column in Wrap or Page." },
            { t: "h", text: "Overlay" },
            { t: "p", text: "Use it when several measurements share the same X, such as two process yields across the same months, and you want them on one picture. Graph Builder: drop both measurements on Y. Add a legend so the series stay distinct." },
            { t: "h", text: "Bubble plot" },
            { t: "p", text: "Use it when a point should move through time and its size should carry a third amount, such as subscriptions by region across years. This is not Graph Builder. Open Graph, then Bubble Plot. Set Y, X (often the year), and an ID so each bubble is one region or one lot." },
            { t: "h", text: "Heat map" },
            { t: "p", text: "Use it for a grid: two categories, and a color that shows the measurement in each cell, such as region by year. Graph Builder: one category on Y, the other on X, then the heat map icon. Color by the measurement." },
            { t: "h", text: "Map" },
            { t: "p", text: "Use it when the question is where, not only how much. If the column is a place name JMP recognizes, such as a country or region, open Graph Builder and use the map shapes. If you have latitude and longitude, open Graph Builder and drag both into the middle of the frame. A local data filter, from the red triangle, steps that map through years or groups without drawing a new graph each time." },
            { t: "h", text: "Shape, and then the audience" },
            { t: "p", text: "A normal quantile plot is a check, not a pass-fail stamp. Points along a straight line look roughly normal. A curve means a tail. The central limit theorem is why averages from even a skewed process can look normal if each average is of enough rows. That is about the average, not about every individual measurement." },
            { t: "p", text: "A graph for yourself can be dense. A graph for someone else answers one practical question and leaves out ink that does not serve it. Customize titles and axes in Graph Builder. Save the steps as a script on the table so you can rerun them. A journal holds the story. People without JMP get a static image, not your live table." },
            { t: "h", text: "The table has to be honest first" },
            { t: "p", text: "Scan for missing cells, impossible values, and names that are the same supplier spelled three ways. Columns Viewer counts missing values. Recode fixes the names, or bins a continuous column on purpose. Most platforms want one row per observation. Stack splits a wide sheet into that shape. Concatenate stacks tables with the same columns. Join matches tables on a shared key, such as an order number." },
            { t: "note", kind: "example", title: "Impurity across reactors", text: "Distribution on impurity shows a right tail. Side-by-side boxes by reactor, in Graph Builder, show one reactor sitting higher. You do not run a test yet. You exclude a row whose temperature was typed as 500, save the Distribution script on the table, and write down what the picture suggested." }
          ],
          checks: [
            {
              id: "dist",
              prompt: "You want a histogram and a box plot of impurity. Where do you start?",
              options: [
                { id: "a", text: "Analyze, then Distribution, with impurity in Y" },
                { id: "b", text: "DOE, then Custom Design" },
                { id: "c", text: "A cause-and-effect diagram" }
              ],
              answer: "a",
              why: {
                a: "Distribution is the platform the demos use for a histogram, a box plot, and the summaries.",
                b: "Custom Design builds an experiment. It does not describe a column you already have.",
                c: "A fishbone lists ideas. It does not read the table."
              }
            },
            {
              id: "hide",
              prompt: "You hide a point but do not exclude it. What changes?",
              options: [
                { id: "a", text: "The graph. The mean still uses the point." },
                { id: "b", text: "The mean. The graph still shows the point." },
                { id: "c", text: "Both the graph and every calculation" }
              ],
              answer: "a",
              why: {
                a: "Hide is visual. Exclude is what takes the row out of the statistics. Use both only when you mean both.",
                b: "That is the opposite of the two commands.",
                c: "Hiding alone does not drop the row from the mean."
              }
            },
            {
              id: "stack",
              prompt: "Five particle measurements sit in five columns, and you need one column of particle size. What do you do?",
              options: [
                { id: "a", text: "Stack the columns so each measurement is a row" },
                { id: "b", text: "Average the five columns and delete the rest" },
                { id: "c", text: "Mark the size column as nominal" }
              ],
              answer: "a",
              why: {
                a: "Most analyses want the observation in the row and the variable in the column. Stack builds that grid.",
                b: "Averaging throws away the five readings the analysis was going to use.",
                c: "The modeling type is a separate decision, and size is continuous."
              }
            },
            {
              id: "run",
              prompt: "Impurity might have drifted as the batches were made. Which graph keeps that order?",
              options: [
                { id: "a", text: "A run chart: Graph Builder, batch on X, impurity on Y, line icon" },
                { id: "b", text: "A histogram of impurity" },
                { id: "c", text: "A tree map of the batch numbers" }
              ],
              answer: "a",
              why: {
                a: "A run chart keeps the sequence. That is the picture for drift, a jump, or a cycle.",
                b: "A histogram shows the shape and throws the batch order away.",
                c: "A tree map sizes categories. Batch numbers are not a size comparison, and the order of manufacture disappears."
              }
            },
            {
              id: "bubble",
              prompt: "You want one bubble per region, moving across years, sized by subscriptions. Where do you open it?",
              options: [
                { id: "a", text: "Graph, then Bubble Plot, with an ID for the region" },
                { id: "b", text: "Analyze, then Distribution" },
                { id: "c", text: "A mosaic plot of region by year" }
              ],
              answer: "a",
              why: {
                a: "Bubble Plot is its own item under Graph. The ID is what makes each bubble one region.",
                b: "Distribution describes columns one at a time. It does not move a point through years.",
                c: "A mosaic shows counts of two categories. It has no bubble moving through time."
              }
            }
          ]
        },
        {
          id: "quality",
          title: "Module 3: Signal, noise, and the gauge",
          lede: "A control chart asks whether the process changed. Capability asks whether it meets the customer. The gauge asks whether you can trust the number.",
          blocks: [
            { t: "p", text: "Common-cause variation is the ordinary wander of a stable process. Special-cause variation is a change: a new lot, a broken fixture, a shift that does something different. A control chart separates the two. You investigate a signal. You do not retune the process for every wiggle inside the limits." },
            { t: "h", text: "Control Chart Builder" },
            { t: "p", text: "One measurement at a time uses an individual and moving-range chart. Rational subgroups, such as five consecutive parts, use an X-bar chart with an R or an S chart. A 3-way chart splits within-sample, between-sample, and over-time variation when the subgroup is not a simple snapshot. Phases draw a break when you change the process, so the limits after the change are not computed from the crisis before it." },
            { t: "p", text: "Tests for special causes flag a point outside the limits and patterns such as a long run on one side of the center. A flag is a question. It is not, by itself, the cause." },
            { t: "h", text: "Capability is a different question" },
            { t: "p", text: "The voice of the customer is the specification limits. Capability indices compare the process spread, and where the process is centered, with those limits. Compute them on a stable process. Short-term and long-term estimates answer different windows of time. A nonnormal measurement needs a method that does not pretend the histogram is a bell. When many characteristics matter, look for the ones that fail first instead of celebrating the ones that pass." },
            { t: "p", text: "The demos calculate indices from Distribution, and a capability analysis with a phase from Control Chart Builder. Read the picture of the fit before you quote a single index." },
            { t: "h", text: "If the gauge is noisy, the process chart is lying" },
            { t: "p", text: "A measurement systems study asks how much of the variation is the instrument and the operator, not the part. Plan the study: parts, operators, repeats. The demo builds the worksheet as a gauge study. Then look: variability charts first, then an EMP analysis or a Gauge R&R. Bias is a separate question from spread. If the gauge eats the tolerance, improve the measurement before you chase the process." },
            { t: "note", kind: "caution", title: "Stable, then capable", text: "A high capability index on a chart full of special causes is a number from a process that is not one process. Hold the chart steady, then talk about the specification." }
          ],
          checks: [
            {
              id: "signal",
              prompt: "A point falls outside the control limits on an individual chart. What does that mean?",
              options: [
                { id: "a", text: "Evidence of a special cause, worth investigating" },
                { id: "b", text: "The process has failed the customer specification" },
                { id: "c", text: "You should recalculate the limits without that point and move on" }
              ],
              answer: "a",
              why: {
                a: "Control limits describe the process's own voice. A point outside them is a signal, not a specification.",
                b: "Specification limits are the customer's voice. They are the capability question.",
                c: "Dropping the point hides the signal. Find out what changed."
              }
            },
            {
              id: "phase",
              prompt: "You changed the feed rate last Tuesday and want to know if the improvement held. What do you add?",
              options: [
                { id: "a", text: "A phase so limits after the change are not mixed with the old process" },
                { id: "b", text: "A fishbone of the old complaints" },
                { id: "c", text: "A mean of all weeks together" }
              ],
              answer: "a",
              why: {
                a: "Phases split the chart at the change. One set of limits would blur the before and the after.",
                b: "A fishbone does not show whether the new feed rate held.",
                c: "One mean across the change answers a question you are no longer asking."
              }
            },
            {
              id: "msa",
              prompt: "Operators measuring the same parts disagree by more than the tolerance. What do you do first?",
              options: [
                { id: "a", text: "Study and improve the measurement system" },
                { id: "b", text: "Tighten the process specification" },
                { id: "c", text: "Run a larger design of experiments on the line" }
              ],
              answer: "a",
              why: {
                a: "If the gauge uses up the tolerance, process experiments will chase measurement noise.",
                b: "A tighter spec does not make a noisy gauge honest.",
                c: "An experiment on the line cannot see a factor smaller than the measurement error."
              }
            }
          ]
        },
        {
          id: "decide",
          title: "Module 4: Intervals, tests, and how many runs",
          lede: "An interval says where a quantity might lie. A test says how surprising the data are if nothing changed. Power says whether you collected enough to see a change you care about.",
          blocks: [
            { t: "p", text: "You have one sample from a larger process. Inference is what you may say about that process, with the uncertainty written down. The module builds three kinds of interval, then the logic of a test, then the tests you actually run, then the sample size you should have chosen first." },
            { t: "h", text: "Three intervals, three questions" },
            { t: "ul", items: [
              "A **confidence interval** is a range for a population mean, at a stated confidence. A 95 percent interval is built so that the method catches the mean in 95 percent of samples. It is not a 95 percent chance attached after the fact to this one interval.",
              "A **prediction interval** is wider. It is for one future observation, which has the process wander plus the uncertainty in the mean.",
              "A **tolerance interval** aims to cover a stated proportion of the population."
            ] },
            { t: "p", text: "In the demos, Analyze, then Distribution, puts the measurement in Y. The interval red triangle is where the confidence, prediction, and tolerance intervals are requested. Alpha is the risk you accept of missing the target. A higher confidence makes a wider interval." },
            { t: "h", text: "A test is a comparison with a stated claim" },
            { t: "p", text: "The null is the claim of no difference, or of a mean equal to a target. The alternative is the departure you are willing to detect. The p-value is how unusual the sample would be if the null were true. A small p-value is evidence against the null. It is not the probability that the null is true, and it is not the size of the effect." },
            { t: "p", text: "One sample against a target: Distribution, then Test Mean. Two groups: Analyze, then Fit Y by X, response in Y and the group in X. Paired data, such as before and after on the same part, is a one-sample test on the differences, not a two-sample test that forgets the pairing. More than two groups: one-way ANOVA in the same Fit Y by X path, then a multiple comparison so you do not declare every pair different just because the overall test moved. An equivalence test asks whether the difference is small enough to ignore, which is a different question from whether it is zero." },
            { t: "h", text: "Practical size, and the sample you need" },
            { t: "p", text: "A tiny shift can be statistically significant in a huge sample and still not matter on the line. State the difference you would act on. Sample size for a confidence interval depends on how narrow you need the interval. Power is the chance you detect that difference when it is real. The demos use JMP's sample-size calculations for a mean, a proportion, a one-sample t test, and two or more means. Do this before you collect, not after a disappointing p-value." },
            { t: "note", kind: "example", title: "Diameter against a target", text: "Distribution on diameter, then a confidence interval, says where the process mean might sit. Test Mean against the target says whether the data look odd if the mean really is the target. A second material is a Fit Y by X of strength by material. You still look at how large the difference is, not only at the p-value." }
          ],
          checks: [
            {
              id: "which",
              prompt: "You want a range for one future part's diameter, not for the mean. Which interval?",
              options: [
                { id: "a", text: "A prediction interval" },
                { id: "b", text: "A confidence interval for the mean" },
                { id: "c", text: "The control limits from last month's chart" }
              ],
              answer: "a",
              why: {
                a: "A prediction interval allows for a new individual, so it is wider than the interval for the mean.",
                b: "The confidence interval is about the mean. A single future part varies more than the mean does.",
                c: "Control limits describe past stability. They are not a prediction interval for the next part."
              }
            },
            {
              id: "pvalue",
              prompt: "A two-sample test returns a p-value of 0.03. What may you say?",
              options: [
                { id: "a", text: "If the two means were equal, data like these would be uncommon" },
                { id: "b", text: "There is a 3 percent chance the means are equal" },
                { id: "c", text: "The difference is large enough to matter on the line" }
              ],
              answer: "a",
              why: {
                a: "That is what a p-value is: surprise, assuming the null.",
                b: "The p-value is not the probability that the null is true.",
                c: "Practical size is a separate look at the difference, not a gift from 0.03."
              }
            },
            {
              id: "pair",
              prompt: "You measure the same ten parts before and after a wash. Which test respects that?",
              options: [
                { id: "a", text: "A paired test on the ten differences" },
                { id: "b", text: "A two-sample test that treats the twenty readings as unrelated" },
                { id: "c", text: "A control chart of the part numbers" }
              ],
              answer: "a",
              why: {
                a: "The pairing is the information. The test belongs on the differences.",
                b: "Ignoring the pair inflates the noise and can hide a real shift.",
                c: "Part numbers are labels. A chart of the labels is not the before-and-after question."
              }
            }
          ]
        },
        {
          id: "regression",
          title: "Module 5: Relationships and models",
          lede: "Correlation describes a straight association. Regression uses one or more columns to explain or predict another, and then you check whether the model deserves that job.",
          blocks: [
            { t: "p", text: "A scatterplot comes first. Correlation is a single number for a linear association between two continuous columns. It does not notice a curve, and one odd point can inflate or erase it. The demos look at the scatter in Graph Builder, then at Fit Y by X and Multivariate. Look at the plot before you quote the number." },
            { t: "h", text: "A straight line" },
            { t: "p", text: "Simple linear regression draws a line so the squared vertical gaps are as small as possible. That is least squares. Fit it from Graph Builder or from Analyze, then Fit Y by X, with the response in Y and the factor in X. The slope is the change in the response for a one-unit change in the factor, inside the range you observed. It is not a license to extrapolate off the edge of the data." },
            { t: "p", text: "The line assumes a straight relationship, similar scatter along the line, and residuals that are not a pattern. Residual plots and a case like Anscombe's quartet are the warning: four pictures can share a line and a correlation and tell four different stories. A curve calls for a polynomial term, fit in Fit Y by X, and you stop adding powers when the picture stops improving." },
            { t: "h", text: "More than one factor" },
            { t: "p", text: "Multiple regression puts several factors in one model. Analyze, then Fit Model. Read the coefficients only after you know what else is in the model. The prediction profiler shows the response as you move one factor and hold the others. Residual plots, and measures of influence, flag points that are pulling the fit. A categorical factor enters as a comparison of levels, not as a fake continuous number. An interaction means the effect of one factor depends on another." },
            { t: "p", text: "Effect Summary is the demo's way to drop terms that are not earning their place. Multicollinearity means factors are standing in for each other, so the individual coefficients get unstable even when the model as a whole still predicts. If two factors move together, do not narrate them as two separate levers." },
            { t: "h", text: "When the response is yes or no" },
            { t: "p", text: "Logistic regression is for a two-level outcome, such as fail or pass. The model talks about the log odds, and the profiler turns that back into a probability. Fit Model again, with the nominal response in Y. Interactions and several factors work the same way as in a linear model. Common failures: complete separation, where a factor predicts the outcome perfectly and the estimates blow up, and a cutoff for classifying pass or fail that you chose without looking at the costs of each mistake." },
            { t: "note", kind: "caution", title: "Explain is not the same as cause", text: "A fitted line on historical data shows association. It does not prove that changing the factor on the line will move the response. That claim wants an experiment, which is the next module." }
          ],
          checks: [
            {
              id: "corr",
              prompt: "Correlation is 0.9, and the scatterplot is a clear curve. What do you trust?",
              options: [
                { id: "a", text: "The plot. The correlation only scores a straight line." },
                { id: "b", text: "The 0.9. A curve this strong must be linear." },
                { id: "c", text: "Neither, until you delete the middle of the data" }
              ],
              answer: "a",
              why: {
                a: "Correlation misses curvature. The scatterplot is the check.",
                b: "A high correlation can sit on a curve or on one odd point.",
                c: "Deleting the middle to force a line is not an analysis."
              }
            },
            {
              id: "slope",
              prompt: "Removal increases by 1.2 units for each unit of outside diameter, inside the observed range. What is that number?",
              options: [
                { id: "a", text: "The slope of the fitted line, not a promise outside the data" },
                { id: "b", text: "The correlation" },
                { id: "c", text: "The p-value" }
              ],
              answer: "a",
              why: {
                a: "The slope is the change in Y per unit of X, where you actually have data.",
                b: "Correlation has no units of removal per unit of diameter.",
                c: "The p-value does not say how large the change is."
              }
            },
            {
              id: "logit",
              prompt: "The outcome is pass or fail. Which model is built for that?",
              options: [
                { id: "a", text: "Logistic regression, with the outcome as a nominal response" },
                { id: "b", text: "A straight-line regression that scores pass as 1 and fail as 0 and treats it as continuous" },
                { id: "c", text: "A correlation between the outcome and a factor" }
              ],
              answer: "a",
              why: {
                a: "Logistic regression models the probability of a two-level outcome.",
                b: "Coding pass and fail as numbers and fitting a line can predict probabilities outside 0 and 1.",
                c: "Correlation wants two continuous columns. An outcome with two names is not one of them."
              }
            }
          ]
        },
        {
          id: "doe",
          title: "Module 6: Design the experiment",
          lede: "Change factors on purpose, together, so you can see effects, curvature, and interactions that one-factor tinkering hides.",
          blocks: [
            { t: "p", text: "An ad hoc change, or one factor at a time, confuses the factor with whatever else drifted that day, and it cannot see an interaction. A designed experiment changes the factors together in a planned table, then fits the results with Fit Model." },
            { t: "h", text: "Words you need before you click" },
            { t: "ul", items: [
              "A **factor** is something you set. A **response** is what you measure.",
              "A **level** is a setting, often a low and a high.",
              "A **run** is one combination you actually execute.",
              "An **effect** is the change in the response tied to a factor. An **interaction** means that change depends on another factor."
            ] },
            { t: "h", text: "Which design" },
            { t: "p", text: "A full factorial runs every combination. DOE, then Classical, then Full Factorial Design. With replication you can estimate noise and test effects. Without replication you use a normal plot of the effects and accept that you cannot separate every small effect from noise. Fractional factorials run a designed fraction when the factor list is long: DOE, then Classical, then Two-Level Screening, then Screening Design. You give up some interactions to save runs. A custom screening design from the Custom Designer is the same idea when the classical table does not fit your constraints." },
            { t: "p", text: "A response surface looks for curvature and a better setting, not only for which factors matter. For a classical central composite, the demo uses DOE, then Classical, then Response Surface Design. The Custom Designer builds the same kind of table when the region is awkward. Sequential work is the usual order: screen, then a surface on the factors that survived, then confirm." },
            { t: "h", text: "Plan the study before the table" },
            { t: "p", text: "Write the objective. Name the responses and how you will measure them. Name the factors and the levels you can actually set. Write the restrictions: hard-to-change factors, mixtures, runs you cannot afford. Randomize the run order unless a restriction forbids it. The anodize case in this module is that planning, done twice: once to choose the design, again to read it." },
            { t: "p", text: "After the runs, Analyze, then Fit Model. The prediction profiler, including a desirability function when several responses matter, shows a compromise setting. Simulating from the profiler is a way to see uncertainty before you commit the line. Then run a confirmation. The model is a map of the region you tested, not of the whole plant." },
            { t: "note", kind: "screen", title: "Analyze what you designed", text: "Full factorial and screening tables are built to be read in Fit Model, with the model the design intended. Do not drop factors because a one-at-a-time plot looked flat, and then pretend the design was smaller." }
          ],
          checks: [
            {
              id: "ofat",
              prompt: "Why is one factor at a time a weak way to improve a process?",
              options: [
                { id: "a", text: "It misses interactions and confuses a factor with whatever else changed that day" },
                { id: "b", text: "JMP cannot plot the results" },
                { id: "c", text: "It uses too many runs" }
              ],
              answer: "a",
              why: {
                a: "Factors that change together can do something neither does alone. One-at-a-time never sees that, and the day itself is a hidden factor.",
                b: "Graph Builder can plot anything. The weakness is the design, not the picture.",
                c: "One-at-a-time often uses fewer runs and learns less per run. The issue is not the count."
              }
            },
            {
              id: "screen",
              prompt: "You have eight factors and budget for a fraction of the full factorial. Where do you start?",
              options: [
                { id: "a", text: "A screening design, classical or custom, to see which factors matter" },
                { id: "b", text: "A response surface on all eight, looking for a peak" },
                { id: "c", text: "A correlation matrix of last year's data" }
              ],
              answer: "a",
              why: {
                a: "Screening spends the runs on finding the active factors. A surface comes after that list is short.",
                b: "A surface on eight factors is a large study aimed at a question you have not earned yet.",
                c: "Historical correlation does not set the factors. It is not an experiment."
              }
            },
            {
              id: "confirm",
              prompt: "The profiler suggests a setting that was not one of the runs. What do you do?",
              options: [
                { id: "a", text: "Confirm it with new runs before you change the line for good" },
                { id: "b", text: "Treat the suggestion as already observed" },
                { id: "c", text: "Average it with the worst run so the result is conservative" }
              ],
              answer: "a",
              why: {
                a: "A suggested optimum is a prediction inside the region. Confirmation is the check.",
                b: "The model interpolated or optimized. That row was not in the table.",
                c: "Averaging with the worst run does not test the prediction."
              }
            }
          ]
        },
        {
          id: "predict",
          title: "Module 7: Predict, and read the text",
          lede: "Hold out rows before you believe a model. Trees, forests, and networks are options next to regression. Text is a column you have to turn into terms first.",
          blocks: [
            { t: "p", text: "Explanatory modeling asks what the factors are doing. Predictive modeling asks whether the model works on rows it did not fit. The split is the whole module. If you score the same rows you trained on, a flexible model will look brilliant and fail on the next week of data. That is overfitting." },
            { t: "h", text: "A validation column" },
            { t: "p", text: "Analyze, then Predictive Modeling, then Make Validation Column. It marks rows as training, validation, and sometimes test. Stratify on an important column so each part of the split still looks like the whole. Fit on training. Choose and compare using validation. Touch the test set once, at the end." },
            { t: "p", text: "For a continuous response, compare prediction error, not the training R-squared. For a pass-or-fail response, a confusion matrix counts the two kinds of mistake, and an ROC curve shows how those counts change as you move the cutoff. The cutoff is a decision about costs. It is not a property of the data alone. Fit Model still fits the linear or logistic model. You simply tell it about the validation column." },
            { t: "h", text: "Other models, same split" },
            { t: "ul", items: [
              "**Partition** grows a decision tree. A classification tree splits nominal outcomes. A regression tree splits a continuous response. A tree with validation stops before it memorizes the training rows.",
              "A **bootstrap forest** grows many trees on resampled rows and is a strong way to rank which columns matter.",
              "A **neural network** stacks layers of simple curves. It can predict well and still be hard to narrate. Fit it with the same validation column. Two layers is already a flexible model. More is not a virtue.",
              "**Generalized regression** fits by maximum likelihood and can penalize extra terms. The lasso shrinks some coefficients to zero, which is a way to drop factors. Compare that model with the others on the validation rows, not on a story about which one sounds more advanced."
            ] },
            { t: "h", text: "Text is unstructured until you curate it" },
            { t: "p", text: "Analyze, then Text Explorer, and put the comment column in the text role. The platform tokenizes, stems, and drops terms you tell it to drop. Curate the list before you trust a theme: spelling, synonyms, phrases. Then explore. Term lists, phrases, and a singular value view show what people are talking about. Mining the text means using those terms as data, not pasting the comments into a word cloud and stopping." },
            { t: "note", kind: "note", title: "Where the course lands", text: "You started with a gap and a process map. You looked at the data, separated signal from noise, and only then tested and modeled. Prediction is the same discipline with a holdout set. A model that cannot survive rows it has not seen is a description of the past, not a decision about the next lot." }
          ],
          checks: [
            {
              id: "holdout",
              prompt: "A neural net fits the training rows almost perfectly and does poorly on the validation rows. What happened?",
              options: [
                { id: "a", text: "It overfit. The validation result is the one that counts." },
                { id: "b", text: "The training fit proves the network found the true process" },
                { id: "c", text: "You should add another layer until validation catches up" }
              ],
              answer: "a",
              why: {
                a: "A flexible model can memorize training rows. Validation is how you see that.",
                b: "A perfect training fit is a warning, not a proof.",
                c: "Another layer makes memorizing easier, not harder."
              }
            },
            {
              id: "tree",
              prompt: "You want a model that shows which columns split a pass-or-fail outcome. Where do you start?",
              options: [
                { id: "a", text: "Analyze, Predictive Modeling, Partition, with the outcome in Y" },
                { id: "b", text: "A control chart of the outcome codes" },
                { id: "c", text: "Text Explorer, even though the columns are numeric" }
              ],
              answer: "a",
              why: {
                a: "Partition builds the classification tree. The splits are the story.",
                b: "A control chart is about time and stability, not about which factors separate pass from fail.",
                c: "Text Explorer wants a text column. Numeric factors are not comments."
              }
            },
            {
              id: "terms",
              prompt: "Survey comments are one text column. What do you do before you interpret a theme?",
              options: [
                { id: "a", text: "Build and curate the term list in Text Explorer" },
                { id: "b", text: "Average the comments" },
                { id: "c", text: "Fit a line with the comment column as a continuous factor" }
              ],
              answer: "a",
              why: {
                a: "Terms, stems, and the words you drop are the data. Curate them before you name a theme.",
                b: "Text has no mean until you have turned it into terms.",
                c: "A comment is not a continuous measurement. The platform for it is Text Explorer."
              }
            }
          ]
        }
      ]
    }
  ]
};
