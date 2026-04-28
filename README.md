# java-etl-demo

A **self-contained, multi-package Java ETL demo** that runs with one command.

- **Pure Java** mapping (no XSLT, no template language)
- **No Maven**, no Gradle, no Docker, no IDE
- **No external libraries** — uses only the JDK
- **No databases or services** to install
- **No internet access** required at runtime
- Reads **4 input formats**, applies **validation + quarantine**, performs **FX conversion**, and emits **5 output documents per accepted order** plus aggregate rollups

## Pipeline at a glance

```
                      ┌─────────────────────────── reference data (CSV) ───────────────────┐
                      │   customer_master   product_master   currency_rates                 │
                      └─────────────────────────────────────────────────────────────────────┘
                                          ▲             ▲             ▲
                                          │             │             │
inputs:                 read              │             │             │
  *.xml   ──┐         ┌───────┐    ┌─────────────────────────────────────────────┐
  *.csv     │         │       │    │  OrderValidator (rules)                     │ errors  ──► output/quarantine/
  *.json    ├──►──────┤  PO   ├───►│   - lines/qty/price                         │ ───────►   *.xml + *.issues.txt
  *.fwf   ──┘         │ POJO  │    │   - currency supported                      │
                      └───────┘    │   - SKU known (warning)                     │
                                   └────────────────┬────────────────────────────┘ ok/warn
                                                    ▼
                                   ┌────────────────────────────────────────────┐
                                   │  PurchaseOrderToSalesOrder                  │
                                   │   + Customer enrichment (code, region)      │
                                   │   + FX conversion to USD                    │
                                   └────────────────┬────────────────────────────┘
                                                    ▼ SalesOrder
                                   ┌──── SalesOrderToInvoice    ──► output/invoices/INV-####.xml
                                   ├──── SalesOrderToShipment   ──► output/shipments/SHIP-####.xml
                                   └──── SalesOrderToInventory  ──► output/inventory_adjustments/ADJ-####.xml
                                                    │
                                                    ▼  routing on grandTotal
                                   ┌───────────────────────────────┐
                                   │  Router  (high_value | std)   │  + JSON twin in sales_orders_json/
                                   └───────────────────────────────┘
                                                    ▼
                                   ┌───────────────────────────────────────────────┐
                                   │  BatchAggregator                              │
                                   │   by_customer / by_region / by_currency /     │
                                   │   by_carrier  CSV rollups                     │
                                   └───────────────────────────────────────────────┘
                                                    ▼
                                   audit.log + summary.csv + aggregates/by_*.csv
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

## Project layout

```
java-etl-demo/
├── README.md
├── .gitignore
├── run.cmd                                     # Windows runner (compiles + runs)
├── run.sh                                      # macOS/Linux runner
├── src/etl/
│   ├── EtlPipeline.java                        # Main orchestrator (5 stages)
│   ├── model/                                  # Typed domain records
│   │   ├── PurchaseOrder.java                  #   source PO + nested POLine
│   │   ├── Customer.java                       #   reference data
│   │   ├── Product.java                        #   reference data
│   │   ├── CurrencyRate.java                   #   reference data (FX)
│   │   ├── ValidationIssue.java                #   one rule violation (warn/error)
│   │   ├── SalesOrder.java                     #   canonical SO + USD total
│   │   ├── Invoice.java                        #   derived: tax + payment terms
│   │   ├── Shipment.java                       #   derived: carrier + weight
│   │   ├── InventoryAdjustment.java            #   derived: stock decrements
│   │   └── BatchSummary.java                   #   aggregated rollup
│   ├── readers/                                # Input → PurchaseOrder POJOs
│   │   ├── XmlPurchaseOrderReader.java
│   │   ├── CsvPurchaseOrderReader.java
│   │   ├── JsonPurchaseOrderReader.java
│   │   ├── MiniJson.java                       #   hand-written JSON parser
│   │   └── SegmentedPurchaseOrderReader.java
│   ├── validators/
│   │   └── OrderValidator.java                 # 7 rules (5 errors + 2 warnings)
│   ├── mappers/                                # Pure-Java transformations
│   │   ├── CustomerLookup.java
│   │   ├── ProductLookup.java
│   │   ├── CurrencyRateLookup.java
│   │   ├── PurchaseOrderToSalesOrder.java      #   PO → SalesOrder + customer + FX
│   │   ├── SalesOrderToInvoice.java            #   SO → Invoice (tax + due date)
│   │   ├── SalesOrderToShipment.java           #   SO → Shipment (carrier rules)
│   │   └── SalesOrderToInventoryAdjustment.java#   SO → Inventory (stock + weight)
│   ├── aggregators/
│   │   └── BatchAggregator.java                # rollups by customer/region/currency/carrier
│   ├── writers/                                # POJOs → XML / JSON / CSV
│   │   ├── XmlSupport.java                     #   shared DOM helper
│   │   ├── JsonSupport.java                    #   shared JSON helper
│   │   ├── SalesOrderXmlWriter.java
│   │   ├── SalesOrderJsonWriter.java
│   │   ├── InvoiceXmlWriter.java
│   │   ├── ShipmentXmlWriter.java
│   │   ├── InventoryAdjustmentXmlWriter.java
│   │   ├── CsvReportWriter.java                #   per-record summary CSV
│   │   └── AggregateReportWriter.java          #   one CSV per dimension
│   └── pipeline/
│       ├── Router.java                         # content-based routing
│       ├── AuditLog.java                       # text audit log writer
│       └── RunRecord.java                      # one row per processed document
├── data/
│   ├── input/
│   │   ├── po_001.xml ... po_005.xml           # XML purchase orders
│   │   ├── po_006_invalid.xml                  # invalid PO (validation demo)
│   │   ├── orders_batch.csv                    # CSV (multi-row per order)
│   │   ├── orders_2026.json                    # JSON array of orders
│   │   └── legacy_orders.fwf                   # EDI-style segmented file
│   └── reference/
│       ├── customer_master.csv                 # Customer code, terms, tax, region
│       ├── product_master.csv                  # SKU → category, weight, taxCategory
│       └── currency_rates.csv                  # Currency → rate to USD
├── bin/                                        # Created at build time
└── output/                                     # Created at runtime
    ├── sales_orders/
    │   ├── high_value/                         # GrandTotal >= 10 000
    │   │   └── PO-####.xml
    │   └── standard/
    │       └── PO-####.xml
    ├── sales_orders_json/                      # JSON twin of every SalesOrder
    │   └── PO-####.json
    ├── invoices/
    │   └── INV-PO-####.xml
    ├── shipments/
    │   └── SHIP-PO-####.xml
    ├── inventory_adjustments/
    │   └── ADJ-PO-####.xml
    ├── quarantine/                             # Rejected by validator
    │   ├── PO-####__source-file.xml            # original source preserved
    │   └── PO-####__source-file.xml.issues.txt # explanation of rejections
    ├── aggregates/
    │   ├── by_customer.csv
    │   ├── by_region.csv
    │   ├── by_currency.csv
    │   └── by_carrier.csv
    ├── audit.log
    └── summary.csv
```

## Pipeline stages

| Stage | What happens |
|------|-------------|
| **0. Reference data** | `CustomerLookup`, `ProductLookup`, and `CurrencyRateLookup` are loaded once from `data/reference/`. |
| **1. Extract** | Lists `*.xml`, `*.csv`, `*.json`, `*.fwf` files in `data/input/` and parses each into typed `PurchaseOrder` records. CSV/JSON/segmented files can each contain multiple orders. |
| **2a. Validate** | `OrderValidator` runs 7 rules (5 ERROR-severity + 2 WARNING). Orders with errors are copied to `output/quarantine/` along with an `*.issues.txt` describing why; the pipeline does not produce any further outputs for them. |
| **2b. Map PO → SalesOrder** | Customer code/region from the lookup, per-line and grand totals, and FX-converted USD amount. |
| **2c. Derive Invoice** | Customer's tax rate × subtotal = tax. Due date = today + payment-terms days. |
| **2d. Derive Shipment** | Carrier + service level chosen by 5-rule policy (FedEx Priority ≥ $50k, FedEx Express ≥ $5k, DHL EU, Royal Mail UK, UPS otherwise). |
| **2e. Derive Inventory Adjustment** | Per-SKU stock decrement using `Product.weightKgPerUnit`. |
| **2f. Route + write** | `Router` classifies SO as HIGH_VALUE / STANDARD. Five artifacts are written: SalesOrder XML, SalesOrder JSON, Invoice XML, Shipment XML, Inventory Adjustment XML. |
| **3. Aggregate** | `BatchAggregator` produces 4 rollup CSVs (by customer, region, currency, carrier). |
| **4. Audit + summary** | Text `audit.log`, spreadsheet-friendly `summary.csv`. |
| **5. Console summary** | Per-record status table, aggregate counts. |

## Validation rules

| Code | Severity | Description |
|------|----------|-------------|
| **E001** | ERROR | Order has no lines |
| **E002** | ERROR | Line quantity is zero or negative |
| **E003** | ERROR | Line unit price is negative |
| **E004** | ERROR | Currency not in `currency_rates.csv` |
| **E005** | ERROR | `OrderDate` not parseable as ISO local date |
| **W001** | WARNING | SKU not in `product_master.csv` |
| **W002** | WARNING | Line quantity exceeds 10 000 (sanity check) |

ERRORs send the order to quarantine. WARNINGs are recorded in the audit log
but processing proceeds normally.

## Customising

- **Routing threshold** — `HIGH_VALUE_THRESHOLD` at the top of `EtlPipeline.java`.
- **Carrier rules** — `chooseCarrier()` in `SalesOrderToShipment.java`.
- **Validation rules** — add or change rules in `OrderValidator.java`.
- **Reference data** — edit the CSVs under `data/reference/`.
- **Add inputs** — drop XML, CSV, JSON, or `.fwf` files into `data/input/`.
- **Reset** — delete `output/` and `bin/` (recreated each run).

## Why "no Maven / no dependencies"?

Every class uses only standard JDK packages:

- `javax.xml.parsers` for DOM parsing
- `javax.xml.transform` for pretty-printing the output XML
- `java.nio.file` for file I/O
- `java.math.BigDecimal` for currency-safe arithmetic
- `java.time.LocalDate` for invoice dating
- `java.lang.Record` (Java 16+) for the domain models

JSON parsing/writing is implemented from scratch in `MiniJson` and
`JsonSupport` (~250 LOC combined) so we don't need Jackson or Gson.

For a production-grade pipeline you would normally swap these for Apache Camel,
Spring Batch, or similar — but this demo deliberately shows the same core ETL
pattern (Validate → Map → Enrich → Route → Load → Aggregate) with zero external
dependencies, so it runs anywhere a JDK does.

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
  </Lines>
</PurchaseOrder>
```

### 2. CSV (multi-row, grouped by OrderId)

```csv
OrderId,Customer,OrderDate,Currency,Sku,Qty,UnitPrice
PO-2001,Cyberdyne Systems,2026-04-17,USD,T800-CHIP,2,8500.00
PO-2001,Cyberdyne Systems,2026-04-17,USD,SKYNET-CORE,1,12500.00
```

### 3. JSON (top-level array of orders)

```json
[
  {
    "orderId": "PO-4001",
    "customer": "ACME Corporation",
    "orderDate": "2026-04-21",
    "currency": "EUR",
    "lines": [
      { "sku": "GADGET-B", "qty": 25, "unitPrice": 49.99 }
    ]
  }
]
```

### 4. Segmented (EDIFACT/IDoc-style)

```
HDR|PO-3001|Genco Trading|2026-04-19|USD
LIN|BOLT-D|500|19.99
LIN|NUT-E|1000|5.50
HDR|PO-3002|Tyrell Corp|2026-04-19|USD
LIN|REPLICANT-X|10|125000.00
```
