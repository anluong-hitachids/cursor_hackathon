# java-etl-demo

A **self-contained, multi-module Java ETL demo** that runs with one command.

- **Pure Java** mapping (no XSLT, no template language)
- **No Maven**, no Gradle, no Docker, no IDE
- **No external libraries** — uses only the JDK
- **No databases or services** to install
- **No internet access** required at runtime
- Reads **3 input formats**, produces **3 output documents per order**, plus an audit log + summary CSV

## Pipeline at a glance

```
                                                       ┌──► output/sales_orders/high_value/PO-####.xml
                                                       │    output/sales_orders/standard/PO-####.xml
data/input/*.xml         ─┐                            │
data/input/*.csv          │   read   ┌─ PurchaseOrder ─┼──► output/invoices/INV-####.xml
data/input/*.fwf         ─┘  ─────►  │   (typed POJO)  │
                                     │        │        └──► output/shipments/SHIP-####.xml
                                     │        ▼
                       data/reference/customer_master.csv ──► enrichment + tax/payment terms
                                                              + carrier rules

                                                       ┌──► output/audit.log
                                                       │
                                                       └──► output/summary.csv
```

## Prerequisites

- **JDK 17+** (recommended LTS). Verify with `java -version`.
  - JDK 16 is the absolute minimum (uses `record` types).
  - Free option: [Adoptium Temurin](https://adoptium.net/).

That's it. No other tools or configuration.

## How to run

### Windows (PowerShell or CMD)

```cmd
cd C:\java-etl-demo
.\run.cmd
```

### macOS / Linux

```bash
cd /path/to/java-etl-demo
chmod +x run.sh
./run.sh
```

The runners compile every `.java` file under `src/` into `bin/`, then launch
`etl.EtlPipeline`. Output goes under `./output/` (cleaned at the start of
each run).

## What you'll see

```
============================================================
  ETL Pipeline starting   batchId=B-20260428-141512
============================================================
  Loading reference data: data\reference\customer_master.csv
    Loaded 9 customer(s)
  Stage 1/4: Extracting input documents from data\input
    XML=5  CSV=1  Segmented=1
  Stage 2/4: Reading + mapping (PO -> SalesOrder, +Invoice, +Shipment)
  Stage 3/4: Routing complete
  Stage 4/4: Writing audit log + summary report

============================================================
  ETL Pipeline finished
============================================================
  Processed:    11
  Successful:   11
  Failed:       0
  High value:   5  -> output\sales_orders\high_value
  Standard:     6  -> output\sales_orders\standard
  Invoices:    11  -> output\invoices
  Shipments:   11  -> output\shipments
  Total value: 318...
  Audit log:    output\audit.log
  Summary:      output\summary.csv

  [OK  ] po_001.xml                         PO-1001    -> STANDARD    USD       449.85  carrier=UPS
  [OK  ] po_002.xml                         PO-1002    -> HIGH_VALUE  USD     13795.00  carrier=FEDEX
  [OK  ] orders_batch.csv#PO-2001           PO-2001    -> HIGH_VALUE  USD     29500.00  carrier=FEDEX
  ...
```

## Project layout

```
java-etl-demo/
├── README.md
├── .gitignore
├── run.cmd                                  # Windows runner (compiles + runs)
├── run.sh                                   # macOS/Linux runner
├── src/etl/
│   ├── EtlPipeline.java                     # Main orchestrator
│   ├── model/                               # Typed domain records
│   │   ├── PurchaseOrder.java               #   source PO + nested POLine
│   │   ├── Customer.java                    #   reference data
│   │   ├── SalesOrder.java                  #   canonical SO + nested SalesItem
│   │   ├── Invoice.java                     #   derived invoice + InvoiceLine
│   │   └── Shipment.java                    #   derived shipment + ShipmentItem
│   ├── readers/                             # Input -> PurchaseOrder POJOs
│   │   ├── XmlPurchaseOrderReader.java
│   │   ├── CsvPurchaseOrderReader.java
│   │   └── SegmentedPurchaseOrderReader.java
│   ├── mappers/                             # Pure-Java transformations
│   │   ├── CustomerLookup.java              #   loads + indexes customer master
│   │   ├── PurchaseOrderToSalesOrder.java   #   PO -> canonical SalesOrder (+ enrichment)
│   │   ├── SalesOrderToInvoice.java         #   SO -> Invoice (tax + due date)
│   │   └── SalesOrderToShipment.java        #   SO -> Shipment (carrier rules)
│   ├── writers/                             # POJOs -> XML / CSV
│   │   ├── XmlSupport.java                  #   shared pretty-print helper
│   │   ├── SalesOrderXmlWriter.java
│   │   ├── InvoiceXmlWriter.java
│   │   ├── ShipmentXmlWriter.java
│   │   └── CsvReportWriter.java
│   └── pipeline/
│       ├── Router.java                      # content-based routing
│       ├── AuditLog.java                    # text audit log writer
│       └── RunRecord.java                   # one row per processed document
├── data/
│   ├── input/
│   │   ├── po_001.xml ... po_005.xml        # XML purchase orders
│   │   ├── orders_batch.csv                 # CSV batch (multi-row per order)
│   │   └── legacy_orders.fwf                # EDI-style segmented file
│   └── reference/
│       └── customer_master.csv              # Customer master data (lookup)
├── bin/                                     # Created at build time
└── output/                                  # Created at runtime
    ├── sales_orders/
    │   ├── high_value/                      # GrandTotal >= 10 000
    │   │   └── PO-####.xml
    │   └── standard/
    │       └── PO-####.xml
    ├── invoices/
    │   └── INV-PO-####.xml
    ├── shipments/
    │   └── SHIP-PO-####.xml
    ├── audit.log
    └── summary.csv
```

## Pipeline stages

| Stage | What happens |
|------|-------------|
| **0. Load reference data** | `CustomerLookup.load()` reads `data/reference/customer_master.csv` and indexes by customer name. |
| **1. Extract** | Lists `*.xml`, `*.csv`, `*.fwf`, `*.txt` files in `data/input/` and parses each into typed `PurchaseOrder` records. CSV rows are grouped by `OrderId`; segmented files use `HDR`/`LIN` records. |
| **2a. Map PO → SalesOrder** | `PurchaseOrderToSalesOrder` enriches the PO with customer code/region from the lookup, computes per-line totals and grand total. |
| **2b. Derive Invoice** | `SalesOrderToInvoice` adds the customer's tax rate, computes tax amount and total due, sets due date = today + payment-terms days. |
| **2c. Derive Shipment** | `SalesOrderToShipment` picks a carrier and service level by amount + region, estimates package weight and count. |
| **3. Route** | `Router` classifies each `SalesOrder` as **HIGH_VALUE** (≥ 10 000) or **STANDARD** and writes it to the appropriate sub-folder. Invoice and Shipment XMLs are written to their own top-level folders. |
| **4. Load** | `AuditLog` writes a text log; `CsvReportWriter` writes a spreadsheet-friendly summary with one row per processed document. |

## Customising

- **Routing threshold** — `HIGH_VALUE_THRESHOLD` at the top of `EtlPipeline.java`.
- **Carrier rules** — `chooseCarrier()` in `SalesOrderToShipment.java`.
- **Tax / payment terms** — edit `data/reference/customer_master.csv`.
- **Add input data** — drop XML, CSV, or segmented files into `data/input/`. CSVs must have header `OrderId,Customer,OrderDate,Currency,Sku,Qty,UnitPrice`.
- **Reset** — delete `output/` (recreated each run) and `bin/` (recompiled each run).

## Why "no Maven / no dependencies"?

Every class uses only standard JDK packages:

- `javax.xml.parsers` for DOM parsing
- `javax.xml.transform` for pretty-printing the output XML
- `java.nio.file` for file I/O
- `java.math.BigDecimal` for currency-safe arithmetic
- `java.time.LocalDate` for invoice dating
- `java.lang.Record` (Java 16+) for the domain models

For a production-grade pipeline you would normally swap these for Apache Camel,
Spring Batch, or similar — but this demo deliberately shows the same core ETL
pattern (Extract → Map → Enrich → Route → Load) with zero external dependencies,
so it runs anywhere a JDK does.

## Input formats reference

### 1. XML (single order per file)

```xml
<PurchaseOrder>
  <Header>
    <OrderId>PO-1001</OrderId>
    <Customer>ACME Corporation</Customer>
    <OrderDate>2026-04-12</OrderDate>
    <Currency>USD</Currency>
  </Header>
  <Lines>
    <Line><Sku>WIDGET-A</Sku><Qty>10</Qty><UnitPrice>19.99</UnitPrice></Line>
    ...
  </Lines>
</PurchaseOrder>
```

### 2. CSV (multi-row, grouped by OrderId)

```csv
OrderId,Customer,OrderDate,Currency,Sku,Qty,UnitPrice
PO-2001,Cyberdyne Systems,2026-04-17,USD,T800-CHIP,2,8500.00
PO-2001,Cyberdyne Systems,2026-04-17,USD,SKYNET-CORE,1,12500.00
PO-2002,Umbrella Corp,2026-04-17,USD,VIRUS-VIAL,5,49.99
...
```

### 3. Segmented (EDIFACT/IDoc-style)

```
HDR|PO-3001|Genco Trading|2026-04-19|USD
LIN|BOLT-D|500|19.99
LIN|NUT-E|1000|5.50
HDR|PO-3002|Tyrell Corp|2026-04-19|USD
LIN|REPLICANT-X|10|125000.00
```
