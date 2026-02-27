## **SoleMate — AR-Based Footwear Try-On Application**

*Final Year Project — 2022–2026*
**Department of Software Engineering**
National University of Computer and Emerging Sciences, Islamabad

### **Project Team**

| Name            | Registration No. | Role / Responsibilities                                                            |
| --------------- | ---------------- | ---------------------------------------------------------------------------------- |
| Ahmad Tashfeen  | 22I-2490         | AR Try-On Module, Multi-Shoe Comparison, AR Optimization, System Integration       |
| Ahmad Tariq     | 22I-1534         | Outfit Matching Engine, Save & Share Feature, Catalog Integration, UI Prototyping  |
| M. Umer Qureshi | 22I-2578         | Personalized Shoe Skins, Try-On Closet, Local Storage Security, Documentation & QA |

**Supervisor:** Ms. Laiba Imran

---

## **1. Introduction**

### **1.1 Problem Statement**

The rise of e-commerce has increased online footwear shopping, yet customers face uncertainty due to the inability to physically try shoes. Size mismatch, comfort concerns, and style visualization challenges lead to high return rates and reduced consumer confidence. Existing sizing charts and static images are insufficient for personalized style decisions.

### **1.2 Motivation**

Online shoppers in Pakistan and Southeast Asia experience low trust when buying footwear digitally. Returns cause financial losses, logistical waste, and customer dissatisfaction. Augmented Reality (AR) provides a real-time, interactive solution to visualize products, improving confidence and reducing return rates. This project bridges research in AR, computer vision, and recommendation systems with practical application in retail.

### **1.3 Proposed Solution**

SoleMate introduces an **AR-based virtual shoe try-on experience** combined with style personalization and outfit matching features.

Key solution capabilities:

| Feature                    | Description                                              |
| -------------------------- | -------------------------------------------------------- |
| **AR Real-Time Try-On**    | Use Snap Camera Kit (Lens Studio) for high-fidelity 3D shoe overlays. |
| **Outfit Matching Engine** | Suggest footwear based on clothing colors and styles.    |
| **Custom Shoe Skins**      | Users design and preview personalized shoe textures.     |
| **Digital Try-On Closet**  | Store and revisit previously tried shoes.                |
| **Smooth & Intuitive UI**  | Minimal learning curve with seamless navigation.         |

### **1.4 Stakeholders**

* **End Users** — Shoppers seeking styling confidence.
* **Retailers / E-Commerce Platforms** — To reduce product returns and increase engagement.
* **Development Team** — Implements AR, UI, and data systems.
* **Supervisors / Academic Evaluators** — Provide direction and assessment.

---

## **2. Project Description**

### **2.1 Scope**

The project focuses on developing a **mobile AR application** for Android, providing:

* Virtual try-on visualization
* Outfit-based recommendations
* Shoe customization features
* Try-on history tracking

Excluded from scope:

* Order placement
* Payment processing
* Delivery/logistics workflows

The app is designed to be **integrable with existing e-commerce platforms** in the future.

---

### **2.2 System Modules**

#### **2.2.1 Core Modules**

| Module                      | Features                                                       |
| --------------------------- | -------------------------------------------------------------- |
| **AR Try-On**               | Snap Camera Kit integration → Lens Loading → 3D Shoe Tracking  |
| **Outfit Matching**         | Extract dominant colors → Recommend complementary shoes        |
| **Personalized Shoe Skins** | Editable shoe templates → Custom textures → Live AR preview    |
| **Try-On Closet**           | Saves last 5 try-ons → Reapply custom skins                    |

#### **2.2.2 Additional Modules**

| Module                    | Features                               |
| ------------------------- | -------------------------------------- |
| **Multi-Shoe Comparison** | Side-by-side shoe evaluation           |
| **Save & Share**          | Capture AR snapshots and share online  |
| **Mix & Match**           | Combine socks and shoes virtually      |
| **Gesture Control**       | Navigate catalog using motion gestures |

### **Figure Placeholder**

```
Figure 2.1: Feature diagram of the proposed AR footwear try-on system.
```

---

### **2.3 Tools & Technologies**

| Category           | Tools / Frameworks                            |
| ------------------ | --------------------------------------------- |
| AR Framework       | Snap Camera Kit SDK (built on ARCore technology) |
| Computer Vision    | Snap AR Engine (Lens Studio Lenses)           |
| Mobile Development | Android Studio (Kotlin/Java)                  |
| 3D Modeling        | Blender, Sketchfab/TurboSquid models          |
| Database / Storage | Firebase Firestore + SQLite (local caching)   |
| Optional Backend   | Node.js + Express, REST APIs, Cloud Functions |

---

### **2.4 Work Division**

| Member       | Responsibilities                                                   |
| ------------ | ------------------------------------------------------------------ |
| **Tashfeen** | AR try-on (Snap Camera Kit) + multi-shoe comparison + AR optimization |
| **Tariq**    | Outfit matching engine + save/share features + catalog integration |
| **Umer**     | Shoe skin customization + try-on history + data storage security   |

---

### **2.5 Timeline**

| Iteration       | Duration | Key Deliverables                                                |
| --------------- | -------- | --------------------------------------------------------------- |
| **Iteration 1** | Aug–Oct  | UI prototype, catalog setup, basic AR overlay                   |
| **Iteration 2** | Nov–Dec  | Improved AR tracking, multi-shoe comparison, outfit suggestions |
| **Iteration 3** | Jan–Feb  | Mix & Match, try-on closet, full custom skins                   |
| **Iteration 4** | Mar–Apr  | Optimization, security, UI polish, final testing, documentation |

---

## **3. Software Architecture (Hybrid Layered Model)**

Here is a **clean, professionally formatted Markdown** version of the section:

---

## **3.1 Architecture Standard Model Selection**

The architecture of the **SoleMate** system follows a refined **Hybrid Layered (N-Tier) Architecture** integrated with selective **cloud-based and serverless components**. This model balances on-device AR performance with scalable backend services, ensuring efficiency, maintainability, and long-term extensibility.

This section reflects the finalized architecture including:
**Node.js Backend-for-Frontend (BFF), Firebase Authentication & Firestore, MongoDB Atlas, Cloud Storage, Cloud Functions, Redis Cache, and the Android Mobile Client.**

---

### **🧩 Recommended Hybrid Architecture — Layered Core + Cloud-Integrated Extensions**

### **1) Overview**

The system combines:

* The **clarity and organization** of a layered architecture for local feature logic.
* The **elastic scalability** of cloud services for data processing and storage.

This approach ensures:

* **⚡ Low latency** for AR operations executed directly on the device.
* **☁ Scalable backend support** for catalog, recommendations, and analytics.
* **🔐 Security and modularity** via Firebase authentication and structured data access.

---

### **2) Diagram (check architecture_diagram.png in /docs folder for more detailed version)**

```
                                      ┌───────────────────────────────┐
                                      │         End User (App)        │
                                      │   Android Client (Kotlin)     │
                                      └───────────────────────────────┘
                                                      │
                                                      │ UI Events / AR Interactions
                                                      ▼
┌───────────────────────────────────────────────────────────────────────────────────┐
│                             Presentation Layer (Client)                            │
│-----------------------------------------------------------------------------------│
│  • Snap Camera Kit Interface                                                       │
│  • Lens Engine & Tracking UI                                                       │
│  • Gesture / Touch Controls                                                        │
│  • ViewModels (MVVM)                                                               │
└───────────────────────────────────────────────────────────────────────────────────┘
                                                      │
                                                      │ Calls Local Feature Logic
                                                      ▼
┌───────────────────────────────────────────────────────────────────────────────────┐
│                         Application / Business Logic Layer                          │
│-------------------------------------------------------------------------------------│
│  • AR Try-On Engine (Snap Lens execution)                                            │
│  • Outfit Matching Engine (color detection + rule-based recommendations)             │
│  • Personalized Shoe Skin Designer (2D → 3D texture mapping)                         │
│  • Try-On History & Cache Manager                                                    │
│  • Multi-Shoe Comparison Engine                                                      │
└───────────────────────────────────────────────────────────────────────────────────┘
                                                      │
                                                      │ Network/API Requests
                                                      ▼
┌───────────────────────────────────────────────────────────────────────────────────┐
│                           Service / Integration Layer                               │
│-------------------------------------------------------------------------------------│
│  • Node.js Backend-for-Frontend (BFF)                                                │
│  • Firebase Authentication (Session & Identity)                                      │
│  • Redis Cache (volatile session & catalog caching)                                 │
│  • Cloud Functions (AI/ML processing, color extraction, recommendations)             │
└───────────────────────────────────────────────────────────────────────────────────┘
                                                      │
                                                      │ Data Access / Asset Retrieval
                                                      ▼
┌───────────────────────────────────────────────────────────────────────────────────┐
│                                    Data Layer                                       │
│-------------------------------------------------------------------------------------│
│  • MongoDB Atlas — Stores product metadata, profiles, try-on history                 │
│  • Firebase Firestore — Session state, lightweight user data                         │
│  • Cloud Storage (S3 / Firebase Storage) — 3D models, textures, images               │
└───────────────────────────────────────────────────────────────────────────────────┘

```

---

### **3) Architecture Composition**

| **Layer**                              | **Key Components**                                                                                           | **Responsibilities**                                                                                              |
| -------------------------------------- | ------------------------------------------------------------------------------------------------------------ | ----------------------------------------------------------------------------------------------------------------- |
| **Presentation Layer (Client)**        | Android App (Kotlin, MVVM), Snap Camera Kit Interface, Gesture & Tap Input    | Handles user interaction, AR rendering via Snap Lenses, and triggers operations via ViewModels.                   |
| **Application / Business Logic Layer** | AR Try-On Engine (Snap), Outfit Matching Engine, Skin Designer, Try-On History | Performs core domain logic such as Lens ID selection, color analysis, and simple catalog queries.                 |
| **Service / Integration Layer**        | Node.js Backend-for-Frontend (BFF), Firebase Auth (hosted), Redis Cache, Cloud Functions (ML/AI processing)  | Validates authentication, aggregates APIs, manages caching, and executes AI workloads.                            |
| **Data Layer**                         | MongoDB Atlas, Firebase Firestore, Cloud Storage (AWS S3 / Firebase Storage)                                 | Stores structured metadata, user session logs, and large binary assets (3D models, textures, media).              |

---

### **4) Cloud & Data Interactions**

* All communication between the Android client and backend goes through the **Node.js BFF**.
* The BFF:

  * Validates Firebase tokens
  * Fetches data from MongoDB Atlas
  * Generates **pre-signed URLs** for assets stored in Cloud Storage

**Data & System Roles:**

| Component                                 | Purpose                                                                                           |
| ----------------------------------------- | ------------------------------------------------------------------------------------------------- |
| **Firebase Auth / Firestore**             | Manages authentication & session metadata                                                         |
| **MongoDB Atlas**                         | Stores product metadata, user profiles, try-on records, and custom skin details                   |
| **Cloud Storage (S3 / Firebase Storage)** | Stores 3D models, textures, and images                                                            |
| **Redis Cache**                           | Holds temporary session/catalog data (volatile, non-persistent)                                   |
| **Cloud Functions**                       | Executes heavy computation (e.g., outfit color extraction, recommendations, thumbnail generation) |

---

### **5) Data Flow Summary**

1. **User logs in** → Firebase Auth validates identity → session recorded in Firestore.
2. **AR Try-On or Outfit Matching** occurs locally, with metadata requests routed to BFF.
3. **Catalog browsing & skin uploads** → stored in MongoDB + Cloud Storage via BFF.
4. **Recommendation requests** → Cloud Function processes → returns suggested results.
5. **Redis cache** accelerates frequently accessed catalog/session data.
6. **Automated backups** → MongoDB Atlas snapshots stored to Cloud Storage.

---

### **6) Key Architectural Constraints**

* **Redis is non-persistent** → only used for caching during active sessions.
* All **user-generated content** (skins, snapshots, designs) is written **directly to MongoDB + Cloud Storage**.
* **AI models** and ML artifacts are stored in **Cloud Storage**, not in the database.
* The **Multi-Shoe Comparison** module relies on direct UI input to avoid gesture conflicts in AR mode.

---

### **7) Advantages of the Hybrid Model**

| Benefit                      | Description                                                      |
| ---------------------------- | ---------------------------------------------------------------- |
| **Modularity & Testability** | Clear separation between UI, logic, and backend services.        |
| **Low AR Latency**           | On-device AR processing reduces network dependency.              |
| **Scalability**              | Serverless cloud components expand automatically with demand.    |
| **Security**                 | Firebase token-based authentication and secure asset streaming.  |
| **Maintainability**          | BFF shields client from backend schema changes.                  |
| **Extensibility**            | Individual services can later grow into microservices as needed. |

---

### **8) Final Verdict**

The **SoleMate** system uses a **Hybrid Layered Architecture** combining:

* A modular **Android client**,
* A **Node.js Backend-for-Frontend**,
* **Firebase Authentication & Firestore**,
* **MongoDB Atlas** for structured data,
* **Redis cache** for performance,
* And **Cloud Functions** for ML-based recommendations.

This design enables:

* **High performance** for AR experiences
* **Scalable and secure cloud infrastructure**
* **Academic rigor + real-world production feasibility**

---

## **4. Conclusion**

SoleMate delivers a **personalized, immersive AR shoe try-on experience**, enhancing user confidence in online footwear shopping while reducing return rates. Its modular, scalable architecture ensures feasibility for academic research and real-world retail deployment.

---

## **References**

* BrandXR (2024). *Augmented Reality in Fashion*.
* IRJET (2024). *AR Shoe Try-On with Real-Time Tracking*.
* Statso (2024). *Fashion Recommendations from Image Features*.