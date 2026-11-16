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
      blurb: "SAS JMP. Start here: open a table, read a distribution, build a graph, test a comparison, fit a line, and lay out a small experiment.",
      lessons: [
        {
          id: "open",
          title: "Open a table and set the columns",
          lede: "Rows are observations. Columns are variables. JMP chooses the analysis from the modeling type, so set that before you trust a mean.",
          blocks: [
            { t: "note", kind: "note", title: "Where this course comes from", text: "The SAS program page at learn.sas.com, program id 180, is not publicly readable. Signed out, it says “You cannot currently access this program” and offers Sign in. No title, outline, or lab list is shown. These lessons follow the public JMP modules in Statistical Thinking for Industrial Problem Solving — exploratory data analysis, decision making with data, correlation and regression, and design of experiments — and the JMP topics named on the public learn.sas.com course that lists JMP: descriptive graphs, t tests, ANOVA, simple and polynomial regression, diagnostics, and a short look at a covariate. Menus match recent JMP." },
            { t: "p", text: "JMP’s unit of work is the data table. File > Open reads CSV, Excel, text, and JMP files, among others. An Excel workbook asks which sheet. After the table opens, the columns panel is on the left. The icon is the modeling type." },
            { t: "ul", items: [
              "**Continuous** (blue triangle): a measurement, such as a quantity or a temperature.",
              "**Nominal** (red bars): categories with no order, such as a warehouse, a vendor, or an invoice number.",
              "**Ordinal** (green bars): categories with an order, such as small, medium, large."
            ] },
            { t: "p", text: "Set the type before you analyze. Click the column name, or use Cols > Column Info, and set Modeling Type. A customer id stored as a number is nominal. Left as continuous, Distribution will offer a mean of id numbers, which is not a quantity of anything." },
            { t: "p", text: "A missing numeric value displays as a dot (·). It is not a zero. A zero is a measured zero. Analyses say how many missing values they excluded. Read that line before you trust N." },
            { t: "ol", items: [
              "File > Open, and choose the file.",
              "Click the column header for the variable you will analyze.",
              "Column Info, then Modeling Type.",
              "OK. Then look at the icon in the columns panel and confirm it."
            ] },
            { t: "note", kind: "example", title: "A shipment sheet", text: "Columns: Customer, Warehouse, Qty, InvoiceNo. Qty is continuous. Customer and Warehouse are nominal. InvoiceNo is nominal even though it looks numeric. A blank Qty is a dot, and it stays out of the mean." }
          ],
          checks: [
            {
              id: "idcol",
              prompt: "Invoice numbers are 10021, 10022, 10023. Which modeling type?",
              options: [
                { id: "a", text: "Continuous, because they are numbers" },
                { id: "b", text: "Nominal. They are labels, not measurements." },
                { id: "c", text: "Ordinal, because the numbers increase" }
              ],
              answer: "b",
              why: {
                a: "JMP will then compute a mean invoice number. That is not a summary of anything you ship.",
                b: "A code is a name. Store it as nominal even when the characters are digits.",
                c: "Ordinal is for an order you mean, such as small then large. Invoice sequence is not a scale of size."
              }
            },
            {
              id: "dot",
              prompt: "A Qty cell shows · . How does the mean treat it?",
              options: [
                { id: "a", text: "As zero" },
                { id: "b", text: "It is excluded. It is not a measured zero." },
                { id: "c", text: "As the column’s maximum, so the gap is obvious" }
              ],
              answer: "b",
              why: {
                a: "A dot is missing. Putting it in as zero pulls the mean down and pretends you measured nothing.",
                b: "The analysis reports how many missing values it set aside. N is the count it kept.",
                c: "JMP does not impute the maximum. If you want a fill, you choose it on purpose."
              }
            },
            {
              id: "where",
              prompt: "Where do you set the modeling type?",
              options: [
                { id: "a", text: "Analyze > Distribution, after the histogram is drawn" },
                { id: "b", text: "Column Info, or the column’s properties, before you analyze" },
                { id: "c", text: "DOE > Custom Design" }
              ],
              answer: "b",
              why: {
                a: "Distribution uses the type you already set. Fix the column, then run it again.",
                b: "Click the column, or Cols > Column Info, and set Modeling Type. The icon in the columns panel should match.",
                c: "Custom Design builds an experiment. It is not where a data column’s type is stored."
              }
            }
          ]
        },
        {
          id: "dist",
          title: "Distributions",
          lede: "Analyze > Distribution describes one column at a time. Compare the mean with the median before you let one number stand for the column.",
          blocks: [
            { t: "p", text: "This is the descriptive half of exploratory data analysis. Analyze > Distribution, and cast the columns into Y. JMP uses the modeling type. A continuous column gives a histogram, quantiles, and summary statistics: N, mean, standard deviation, and the quantiles, including the median. Turn on the outlier box plot from the red triangle next to the column: Display Options > Outlier Box Plot." },
            { t: "p", text: "Compare the mean and the median. A mean pulled above the median, with a long right tail, is right skew. Points beyond the box plot’s whiskers are JMP’s outlier rule for that plot. They are a description, not a verdict that the row is an error. Click a histogram bar and the matching rows highlight in the table. That link is how you find which shipments the bar is." },
            { t: "p", text: "A nominal column gives a frequency bar chart and counts, not a mean. The red triangle on the report is the local menu for that analysis." },
            { t: "ol", items: [
              "Analyze > Distribution.",
              "Put Qty in Y, and OK.",
              "Red triangle beside Qty > Display Options > Outlier Box Plot.",
              "Click the tall bar on the right and read the highlighted rows in the table."
            ] },
            { t: "hist", caption: "Qty on 30 shipment lines. Most lines are small. One bar sits far to the right, and it pulls the mean (42) away from the median (18).", bins: [
              { label: "0–10", n: 9 },
              { label: "10–20", n: 8 },
              { label: "20–40", n: 5 },
              { label: "40–80", n: 3 },
              { label: "80–200", n: 3 },
              { label: "200+", n: 2 }
            ] },
            { t: "note", kind: "example", title: "How to read this picture", text: "Mean 42, median 18, one bar far to the right. The mean is not a typical line. Open the high bar, see whether it is one invoice, and decide whether it belongs. Until you decide, the median is the more honest one-number summary." }
          ],
          checks: [
            {
              id: "skew",
              prompt: "Mean 42, median 18, and the histogram has a long right tail. What is going on?",
              options: [
                { id: "a", text: "A few large values are pulling the mean above a typical line" },
                { id: "b", text: "The column is nominal, so both numbers are category codes" },
                { id: "c", text: "The median is always larger than the mean" }
              ],
              answer: "a",
              why: {
                a: "Right skew. Look at the rows in the far bar before you quote the mean as “the” shipment size.",
                b: "A nominal column would not offer a mean and a median of the quantity. Those are continuous summaries.",
                c: "Here the median is smaller. That is the clue."
              }
            },
            {
              id: "counts",
              prompt: "You want a count per warehouse, not a mean of warehouse codes. What must Warehouse be?",
              options: [
                { id: "a", text: "Continuous" },
                { id: "b", text: "Nominal, so Distribution draws frequencies" },
                { id: "c", text: "Left unset, so JMP can choose after the plot" }
              ],
              answer: "b",
              why: {
                a: "Continuous warehouse codes produce a numeric distribution of the codes. That is not a count of shipments.",
                b: "Nominal (or ordinal, if you truly have an order) gives the frequency chart.",
                c: "The type is already set, often by how the column was imported. Check the icon first."
              }
            },
            {
              id: "click",
              prompt: "You click a histogram bar and rows highlight in the table. What is JMP showing?",
              options: [
                { id: "a", text: "The observations that fell in that bar" },
                { id: "b", text: "Every row that is missing" },
                { id: "c", text: "The design points of an experiment" }
              ],
              answer: "a",
              why: {
                a: "The table and the graph are linked. The highlighted rows are that bar.",
                b: "Missing values are the dots, reported as excluded. A bar is a range of measured values.",
                c: "Design points come from DOE. This click is just the rows behind one bin."
              }
            }
          ]
        },
        {
          id: "graph",
          title: "Graph Builder",
          lede: "Graph > Graph Builder is the picture you design by dragging. The element you pick decides whether you see points, bars, or a line.",
          blocks: [
            { t: "p", text: "Exploratory data analysis is not finished when one column has a histogram. Graph Builder puts columns in roles. Drop a column on X, Y, Color, Overlay, Group X, or Group Y. The element icons along the top choose points, a smoother, a line, bars, a box plot, and others. Click Done when the graph should stop responding to drags. Until then you are still designing." },
            { t: "p", text: "A useful first graph of the shipment table: Warehouse on X (nominal), Qty on Y (continuous), element set to Bar. JMP summarizes Y for each X. Drag Customer to Overlay or Color when you need the bars split. If the picture is a cloud of points, you asked for points. Switch the element. The same two columns do not decide the geometry." },
            { t: "p", text: "Read the axis titles. They are the columns you dropped. If Qty is summed, a warehouse with one huge invoice dominates the axis. Click or brush to see which rows. Color is a third variable. It is not decoration if it changes what you can compare." },
            { t: "ol", items: [
              "Graph > Graph Builder.",
              "Drag Warehouse to X and Qty to Y.",
              "Click the bar element.",
              "Drag Customer to Overlay.",
              "Click Done."
            ] }
          ],
          checks: [
            {
              id: "bars",
              prompt: "You see one bar per warehouse. Which element is active?",
              options: [
                { id: "a", text: "Bar, with a nominal X and a continuous Y" },
                { id: "b", text: "A fitted line from Fit Y by X" },
                { id: "c", text: "A histogram of a single continuous column" }
              ],
              answer: "a",
              why: {
                a: "That is Graph Builder’s bar element summarizing Qty by Warehouse.",
                b: "A fitted line is a regression plot, one cloud and a line, not one bar per warehouse.",
                c: "A histogram is Distribution, bins of one column, not a bar per category."
              }
            },
            {
              id: "points",
              prompt: "You wanted each shipment as a point, and you see a single bar per warehouse. What do you change?",
              options: [
                { id: "a", text: "The element, from bar to points" },
                { id: "b", text: "The modeling type of Qty to nominal" },
                { id: "c", text: "File > Open, because the table is the wrong shape" }
              ],
              answer: "a",
              why: {
                a: "The columns can stay. The element is what drew the bars. Switch it, and don’t click Done until the picture is the one you meant.",
                b: "Making Qty nominal would throw away the measurement. You would get counts of quantity-codes.",
                c: "The table is fine. The graph element is the choice."
              }
            },
            {
              id: "done",
              prompt: "Why click Done?",
              options: [
                { id: "a", text: "It runs a hypothesis test on the bars" },
                { id: "b", text: "It finishes the graph so later drags do not rebuild it" },
                { id: "c", text: "It sets every column to continuous" }
              ],
              answer: "b",
              why: {
                a: "Graph Builder draws. Tests are Analyze > Fit Y by X or Distribution.",
                b: "Until Done, you are still in the design surface.",
                c: "Done does not touch modeling types. Those stay on the columns."
              }
            }
          ]
        },
        {
          id: "tests",
          title: "Intervals and tests",
          lede: "You rarely pick “t test” from a menu. You put a continuous Y and a grouping X in Fit Y by X, and you read the difference as well as the p-value.",
          blocks: [
            { t: "p", text: "Decision making with data starts from a question: is this mean where we think it is, or do these groups differ by enough to matter? A confidence interval is the range of values still compatible with the data at the stated confidence. A p-value is the probability of a result at least this far from the null — no difference, or no shift — if that null were true. A small p is evidence against the null. It is not the probability that the null is true, and it is not the size of the effect. Read the difference in means beside it." },
            { t: "p", text: "Sample size and power travel together. A tiny study can miss a difference you would care about. A huge study can declare a trivial difference “significant.” The public JMP path treats sample size as part of the decision, not as an afterthought. JMP’s DOE and sample-size tools ask for the difference you care about and then tell you how many runs that implies. This lesson does not compute a sample size. It asks you not to quote a p-value without the size of the effect." },
            { t: "h", text: "Which platform" },
            { t: "ul", items: [
              "**One sample.** Analyze > Distribution on a continuous column, red triangle > Test Mean, and type the mean you are comparing with. Read the test and the confidence interval for the mean.",
              "**Two samples.** Analyze > Fit Y by X. Y continuous, X nominal with two levels. JMP offers the two-group t test.",
              "**Paired.** Each unit was measured twice. Stack the two measurements so the difference is the continuous column, and test that difference against zero. Or give Fit Y by X a matched-pairs role when you have arranged the two columns that way. Do not run a two-sample test on paired data. The pairing is the point.",
              "**More than two groups** is the next lesson: one-way ANOVA."
            ] },
            { t: "ol", items: [
              "Confirm modeling types. Scrap continuous, Machine nominal.",
              "Analyze > Fit Y by X.",
              "Y = Scrap, X = Machine, OK.",
              "Read the two means, the difference, the interval, and the p-value."
            ] },
            { t: "outline", title: "Oneway Analysis of Scrap By Machine", rows: [
              { text: "Means", head: true },
              { text: "Machine A — 2.1", indent: true },
              { text: "Machine B — 3.4", indent: true },
              { text: "Difference — 1.3", indent: true },
              { text: "t Test", head: true },
              { text: "p-value — 0.03", indent: true },
              { text: "The confidence interval for the difference excludes 0", indent: true }
            ] },
            { t: "note", kind: "example", title: "How to read that report", text: "There is evidence the machines differ: the interval for the difference excludes 0, and the p-value is 0.03. Whether 1.3 points of scrap is worth a maintenance stop is the process decision. The p-value does not make it. If Machine had been left continuous, Fit Y by X would fit a line through machine numbers instead of comparing groups." }
          ],
          checks: [
            {
              id: "which",
              prompt: "Y is continuous. X is nominal with 2 machines. Which analysis does Fit Y by X give you?",
              options: [
                { id: "a", text: "A two-group t test" },
                { id: "b", text: "A chi-square test of two nominal columns" },
                { id: "c", text: "A designed experiment" }
              ],
              answer: "a",
              why: {
                a: "Continuous Y and a two-level nominal X is the two-sample t test. Three or more levels become a one-way ANOVA.",
                b: "Chi-square is two nominal columns. Here Y is a measurement.",
                c: "An experiment is DOE > Custom Design, before the data exist. This table is already measured."
              }
            },
            {
              id: "pval",
              prompt: "The p-value is 0.03. Which sentence is fair?",
              options: [
                { id: "a", text: "There is a 3 percent chance the machines are the same." },
                { id: "b", text: "Results at least this far apart would be unusual if the machines had the same mean." },
                { id: "c", text: "Machine B is 3 percent worse than Machine A." }
              ],
              answer: "b",
              why: {
                a: "The p-value is not the probability that the null is true.",
                b: "That is the null-based reading. Then look at the 1.3 difference and ask whether it matters.",
                c: "1.3 is the difference in means. 0.03 is not a percent change."
              }
            },
            {
              id: "paired",
              prompt: "Each batch was measured before and after a change. Which test respects the pairing?",
              options: [
                { id: "a", text: "A two-sample t test that ignores which before goes with which after" },
                { id: "b", text: "A test of the paired differences against zero" },
                { id: "c", text: "Distribution of the invoice numbers" }
              ],
              answer: "b",
              why: {
                a: "Unpaired, the batch-to-batch noise swamps the change you actually made.",
                b: "The difference on each batch is the observation. Test that mean against zero.",
                c: "Invoice numbers are labels. They are not the before-and-after measurement."
              }
            }
          ]
        },
        {
          id: "models",
          title: "ANOVA, a line, and whether the fit is trustworthy",
          lede: "Fit Y by X compares groups and fits one line. When the scatter bends, or more than one X matters, you look harder before you believe the slope.",
          blocks: [
            { t: "p", text: "Continuous Y and a nominal X with three or more levels is a one-way ANOVA, still in Analyze > Fit Y by X. You get a mean per group and a test that the means differ. Two levels was the t test. The platform did not change. The number of levels did. An N-way analysis, two grouping columns at once, is Analyze > Fit Model, with both nominal columns in the model. Fit Model is also where a continuous covariate joins a group: that is covariance analysis, a group comparison adjusted for a measurement such as batch size. Put the group and the covariate in the model and read whether the group effect remains after the covariate is accounted for." },
            { t: "p", text: "Two continuous columns start as a scatterplot. Graph Builder, or Fit Y by X with both columns continuous, shows the cloud. Correlation is the straight-line association, between −1 and 1, and it misses a curve. If the cloud bends, a polynomial term — the column and its square — can follow the bend. A straight line through a curve will look “significant” and still be the wrong shape." },
            { t: "p", text: "More than one predictor is Analyze > Fit Model. After the fit, look at residuals: the vertical gaps between the points and the fit. A pattern left in the residuals means the line was too simple. One point with a huge residual, or a point far out on X that swings the slope, is influential. Decide whether it is a real batch or a bad row. Do not delete it to improve the p-value. Collinearity means two predictors move together, so the fit cannot tell their slopes apart. The coefficients get unstable. That is a reason to drop or combine a predictor, not a reason to quote both slopes to three decimals." },
            { t: "ol", items: [
              "Confirm Y is continuous and the grouping column is nominal.",
              "Analyze > Fit Y by X. Y in Y, the group in X, OK.",
              "For a line: both columns continuous. Read the slope, the interval, and the scatter, not only the p-value.",
              "Red triangle options for residual plots when you are in Fit Model with more than one X."
            ] },
            { t: "note", kind: "example", title: "A bend and a point", text: "Yield against temperature rises, then flattens. A straight line’s p-value is small because the left side is lower than the right side, and the line still misses the flatten. Add the square of temperature, or simply refuse the straight summary. Separately, one run at an extreme temperature and a wild yield swings the slope. The residual plot shows it. Check the lab book before you drop the row." }
          ],
          checks: [
            {
              id: "anova",
              prompt: "Y is continuous. X is nominal with 4 suppliers. What do you get from Fit Y by X?",
              options: [
                { id: "a", text: "A one-way ANOVA" },
                { id: "b", text: "A paired t test" },
                { id: "c", text: "A chi-square test" }
              ],
              answer: "a",
              why: {
                a: "More than two levels of a nominal X, with a continuous Y, is one-way ANOVA. Two levels would have been a t test.",
                b: "Paired needs two measurements on the same unit. Four suppliers are four groups.",
                c: "Chi-square needs a nominal Y as well. Yield is continuous."
              }
            },
            {
              id: "bend",
              prompt: "The scatter of yield against temperature rises and then flattens. A straight line has a small p-value. What is the fair reading?",
              options: [
                { id: "a", text: "The straight line is confirmed, because the p-value is small" },
                { id: "b", text: "The line is the wrong shape. Look at a curve, or don’t summarize with one slope." },
                { id: "c", text: "Change temperature to nominal so the p-value goes away" }
              ],
              answer: "b",
              why: {
                a: "A small p-value says a straight association is unlikely to be noise. It does not say the straight shape is right. The picture shows a bend.",
                b: "Polynomial regression, or a smoother in Graph Builder, follows the flatten. Then read the curve.",
                c: "Temperature is a measurement. Hiding it in categories throws away the shape you can see."
              }
            },
            {
              id: "coli",
              prompt: "Two predictors always rise together, and both slopes jump around when you drop a few rows. What is that called?",
              options: [
                { id: "a", text: "Collinearity. The fit cannot separate their effects." },
                { id: "b", text: "A paired t test" },
                { id: "c", text: "A missing-value dot" }
              ],
              answer: "a",
              why: {
                a: "That is collinearity. Combine or drop one predictor rather than quoting both unstable slopes.",
                b: "A paired t test is two measurements on one unit. This is a regression with two tangled X columns.",
                c: "A dot is a missing cell. It does not make two slopes trade off."
              }
            }
          ]
        },
        {
          id: "doe",
          title: "A first experiment",
          lede: "Changing one factor at a time cannot see an interaction. A designed experiment changes the factors together, in a table JMP builds before you run anything.",
          blocks: [
            { t: "p", text: "A factor is something you set, such as temperature, pressure, or supplier. A response is what you measure, such as yield. A run is one combination you actually execute. An interaction is a factor whose effect depends on another factor: a temperature that only matters at one pressure. One-factor-at-a-time experiments miss that, and they spend runs on a weaker estimate." },
            { t: "p", text: "JMP’s doorway is DOE > Custom Design. You name the response and a goal (maximize, match a target, or minimize), you add factors with their lows and highs or their categories, you accept or set the number of runs, and you make a table. Each row is one run. Factor columns are filled with the settings. The response column is empty until you execute the run and type what you measured. Run the rows you were given. Randomize the execution order when the design says so. Do not sort the table into a tidy temperature order and then forget that the order was part of the protection against drift." },
            { t: "ol", items: [
              "DOE > Custom Design.",
              "Add the response and set the goal.",
              "Add factors. Continuous factors get a low and a high. Categorical factors get levels.",
              "Continue. Set or accept the number of runs.",
              "Make Design, then Make Table.",
              "After the response is filled in: Analyze > Fit Model. The model is usually prepared from the design."
            ] },
            { t: "p", text: "Effect Summary lists factors by evidence. A larger LogWorth, and a smaller p-value, means stronger evidence that the factor’s effect is not just noise. A LogWorth around 2 is the neighborhood of a p-value of 0.01. The Prediction Profiler shows the predicted response as you drag a factor. A steep trace is a strong effect inside the range you tried. A flat trace is a factor that, in that range, did little. If two factors interact, the trace of one changes when you move the other." },
            { t: "outline", title: "Effect Summary, after the response is filled in", rows: [
              { text: "Temperature — LogWorth well above 2", head: true },
              { text: "Pressure — p-value 0.62", head: true },
              { text: "Supplier — weakest of the three", head: true }
            ] },
            { t: "note", kind: "example", title: "How to read it", text: "The profiler: yield rises as you drag temperature from its low to its high, and it barely moves when you drag supplier. Pressure’s trace is flat. Next, spend samples on temperature, not on switching supplier, unless a later interaction says supplier changes the temperature slope. A flat trace is not proof that pressure never matters outside the range you tested." },
            { t: "p", text: "This is the doorway the public JMP path opens: language, a design table, Effect Summary, and the profiler. It is not a full design course. Power, response-surface optimization, and split plots start from the same place, a design table and Fit Model. Quality methods — control charts, capability, measurement systems — and predictive modeling are the other public modules, and they are outside this short course." }
          ],
          checks: [
            {
              id: "row",
              prompt: "What is a row in the design table before the experiment?",
              options: [
                { id: "a", text: "One planned run: factor settings filled in, response still empty" },
                { id: "b", text: "A fitted slope" },
                { id: "c", text: "A journal entry" }
              ],
              answer: "a",
              why: {
                a: "You execute that row and then type the response you measured. The settings were the instruction.",
                b: "Slopes appear after Fit Model, once the response column is filled in.",
                c: "Journal entries are Priority. A JMP design table is a plan of runs."
              }
            },
            {
              id: "menu",
              prompt: "Where do you start?",
              options: [
                { id: "a", text: "Analyze > Distribution" },
                { id: "b", text: "DOE > Custom Design" },
                { id: "c", text: "Graph > Graph Builder, and click Done" }
              ],
              answer: "b",
              why: {
                a: "Distribution describes a column you already have. The design table does not exist yet.",
                b: "Custom Design is the path: response, factors, runs, Make Table.",
                c: "Graph Builder pictures data. It does not build the run list."
              }
            },
            {
              id: "flat",
              prompt: "Pressure’s profiler trace is flat, and its p-value is 0.62. What may you conclude?",
              options: [
                { id: "a", text: "Little evidence that pressure changed the response inside the range you tested" },
                { id: "b", text: "Pressure never matters at any setting" },
                { id: "c", text: "The experiment failed and the table should be discarded" }
              ],
              answer: "a",
              why: {
                a: "Flat, and a large p-value, means little evidence inside the lows and highs you tried. Say it that way.",
                b: "You did not test outside that range. A flat trace is not a universal claim.",
                c: "A clear temperature effect and a flat pressure effect is a result. Keep the table."
              }
            }
          ]
        }
      ]
    }
  ]
};
