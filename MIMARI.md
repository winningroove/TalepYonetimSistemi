# Mimari ve Uygulama Arayüzü (API) Dokümanı

Bu belge, Talep Yönetim Sistemi'nin katmanlı mimarisini, uygulama uç noktalarını (route'lar),
servis katmanı "iç API"sini ve veritabanı şemasını açıklar.

---

## 1. Genel Mimari

Uygulama, **Vaadin Flow** tabanlı sunucu-taraflı bir **monolit**tir. Klasik istemci-sunucu
ayrımı yerine, UI Java ile sunucuda üretilir ve tarayıcıyla Vaadin'in kendi iç protokolü
(WebSocket/HTTP + `@Push`) üzerinden senkronize olur.

```
┌─────────────────────────────────────────────────────────────┐
│  Tarayıcı (Vaadin istemci — otomatik üretilir)               │
└───────────────▲─────────────────────────────────────────────┘
                │  Vaadin iç protokolü (HTTP + WebSocket/@Push)
┌───────────────┴─────────────────────────────────────────────┐
│  SUNUCU (Spring Boot)                                         │
│                                                              │
│  views/         → Vaadin ekranları (@Route). Kullanıcı        │
│                   etkileşimini dinler, servisleri çağırır.    │
│      │                                                        │
│      ▼                                                        │
│  *Service        → İş mantığı, doğrulama, önbellek, olay      │
│   (@Service)       kancaları (bildirim/mesaj tetikleme).      │
│      │                                                        │
│      ▼                                                        │
│  *Repository     → JdbcTemplate ile Oracle'a SQL. RowMapper.  │
│   (@Repository)                                               │
│      │                                                        │
│      ▼                                                        │
│  Oracle Database (Eren_* tabloları)                          │
└─────────────────────────────────────────────────────────────┘
```

**Katman sorumlulukları:**

| Katman | Sorumluluk | Örnek |
|---|---|---|
| **View** (`views/`) | Ekran çizimi, kullanıcı etkileşimi, güvenlik anotasyonu | `POView`, `CustomerView` |
| **Service** (`*Service`) | İş kuralları, doğrulama, önbellekleme, olay tetikleme | `RequestService`, `WorkflowService` |
| **Repository** (`*Repository`) | Ham SQL (JdbcTemplate + RowMapper) | `RequestRepository` |
| **Model** | Veri taşıyıcı (Lombok `@Data`) | `Request`, `Workflow` |
| **Enum** (`enums/`) | Sabit kümeler | `RequestStatus`, `Role` |

> **Not:** JPA/Hibernate kullanılmaz. Tüm veri erişimi `JdbcTemplate` + elle yazılmış
> `RowMapper`'lar üzerinden yapılır. Servisler `findById` gibi okuma metotlarında
> `ConcurrentHashMap` tabanlı basit önbellek tutar; her yazma işleminde önbellek temizlenir.


## 2. Uygulama Uç Noktaları (Route Tablosu)

Tüm ekranlar `@Route` ile tanımlanır. Erişim, Spring Security + Vaadin
`NavigationAccessControl` tarafından view üzerindeki anotasyonlara göre denetlenir.

| HTTP Yolu | View sınıfı | Erişim (anotasyon) | Açıklama |
|---|---|---|---|
| `/` | `RootView` | `@AnonymousAllowed` | Girişliyse role göre yönlendirir, değilse `/login` |
| `/login` | `LoginView` | `@AnonymousAllowed` | Giriş formu (Türkçe hata mesajları) |
| `/customer` | `CustomerView` | `@RolesAllowed("CUSTOMER")` | Müşteri paneli: talep oluştur/izle, mesajlaş |
| `/po` | `POView` | `@RolesAllowed("PRODUCT_OWNER")` | Ürün sorumlusu: gösterge paneli, önceliklendirme, kopya birleştirme |
| `/scrum-master` | `ScrumMasterView` | `@RolesAllowed("SCRUM_MASTER")` | Sprint board, geliştirici atama, çaba tahmini |
| `/developer` | `DeveloperView` | `@RolesAllowed("DEVELOPER")` | Görevlerim, durum ilerletme, SM'e geri gönderme |
| `/admin` | `AdminView` | `@RolesAllowed("ADMIN")` | Kullanıcı ve şirket yönetimi |
| `/logout` | (Spring Security) | — | Oturumu kapatır, `/login`'e döner |

**Kimlik doğrulama:** Form login. `UserDetailsService` kullanıcıyı `Eren_users`'tan e-posta ile
bulur; pasif hesaplar reddedilir. Şifreler **BCrypt** ile saklanır. Roller
`ROLE_<ROL>` yetkisi olarak verilir (bkz. [`SecurityConfig`](src/main/java/com/example/security/SecurityConfig.java)).

---

## 3. Uygulama Servis Katmanı ("İç API")

View'ların çağırdığı iş mantığı metotları. Bir REST API'ye en yakın karşılık budur.

### 3.1 `RequestService` — Talep yaşam döngüsü
| Metot | Açıklama |
|---|---|
| `createRequest(Request)` | Yeni talep (durum=NEW). Tüm PO'lara bildirim gönderir. |
| `getCustomerRequests(Long customerId)` | Bir müşterinin talepleri. |
| `findById(Long requestId)` | Tek talep (önbellekli). |
| `getAllActiveRequests()` | Aktif talepler (REJECTED/DUPLICATE hariç). |
| `getByStatus(RequestStatus)` | Duruma göre talepler. |
| `takeUnderReview(Long)` | NEW → UNDER_REVIEW. Müşteriye bildirim. |
| `markAsPrioritized(Long)` | UNDER_REVIEW → PRIORITIZED. Müşteriye bildirim. |
| `rejectRequest(Long, String gerekce)` | Talebi reddeder (gerekçe zorunlu). Müşteriye bildirim. |
| `revertToUnderReview(Long)` | PRIORITIZED → UNDER_REVIEW (SM→PO geri gönderme). |
| `updateYoneticiTakdiri(Long, YoneticiTakdiri)` | Yönetici takdir puanını günceller. |
| `getCredibilityStats(Long customerId)` | Talep sahibinin geçmiş onay/ret istatistiği. |
| `findPotentialDuplicates(Request)` | Aynı şirketten, aynı başlıklı olası kopyalar. |
| `mergeDuplicate(Long anaId, Long kopyaId)` | Kopyayı ana talebe bağlar (durum=DUPLICATE). |

### 3.2 `WorkflowService` — İş akışı / görev
| Metot | Açıklama |
|---|---|
| `createWorkflow(Long requestId)` | Talebi iş akışına alır (durum=BACKLOG). Müşteriye bildirim. |
| `findByRequestId(Long)` | Talebin iş akışı (önbellekli). |
| `getDeveloperWorkflows(Long devId)` | Geliştiricinin aktif görevleri. |
| `getUnassignedWorkflows()` | Atanmamış görevler. |
| `getAllActiveWorkflows()` / `getAllWorkflows()` | Aktif / tüm görevler. |
| `getDoneWorkflowsByDeveloper(Long devId)` | Geliştiricinin tamamladıkları. |
| `assignDeveloperBySM(Long taskId, Long devId, int version)` | Geliştirici atar (iyimser kilit). Geliştiriciye bildirim. |
| `updateStatus(Long taskId, WorkflowStatus, int version)` | Durum ilerletir. DONE olunca müşteriye bildirim. |
| `sendBackToScrumMaster(Long taskId, int version, Long byUserId, String gerekce)` | Başlanmış görevi BACKLOG'a döndürür, SM'lere bildirim, gerekçeyi ekip notlarına yazar. |
| `sendBackToProductOwner(Long taskId, Long byUserId, String gerekce)` | BACKLOG görevini iş akışından siler, talebi UNDER_REVIEW'e döndürür, PO'lara bildirim. |

### 3.3 `PrioritizationService` — Önceliklendirme skoru
| Metot | Açıklama |
|---|---|
| `findByRequestId(Long)` | Talebin önceliklendirme kaydı (önbellekli). |
| `savePrioritizationByPO(Prioritization, Long customerId, LocalDateTime createdAt)` | PO değerlerini kaydeder, baz skoru hesaplar. |
| `updateGelistiriciMudahalesi(Long requestId, GelistiriciMudahalesi)` | SM çaba tahminini ekler, nihai skoru hesaplar. |
| `calculateBeklemeSuresiPuan(LocalDateTime)` | Bekleme süresine göre puan. |
| `calculateCredibilityScore(Long customerId)` | Talep sahibi güvenilirlik puanı (±). |
| `calculateFinalScore(Prioritization, YoneticiTakdiri, int credibility)` | Nihai skoru döndürür. |
| `getLabel(int skor)` | Skoru etikete çevirir (Çok Düşük … Kritik). |

**Skor formülü:** `bazSkor = (isEtkisi·30 + aciliyet·25 + musteriDegeriPuan·20 + isTipiPuan·15 + beklemeSuresiPuan·10) / 5`,
ardından çaba tahmini, yönetici takdiri ve güvenilirlik puanı eklenir.

### 3.4 `UserService` / `CompanyService` — Kimlik ve şirket
| Metot | Açıklama |
|---|---|
| `UserService.findByEmail(String)` | Girişte kullanıcı çözümü. |
| `UserService.findById(Long)` / `findAllActive()` / `findActiveByRole(Role)` | Kullanıcı sorguları (önbellekli). |
| `UserService.createUser(User, String sifre)` | Kullanıcı oluşturur (alan doğrulama + BCrypt). |
| `UserService.updateUser(...)` / `setActive(...)` / `deleteUser(Long)` | Güncelleme/pasifleştirme/silme (referans koruması). |
| `UserService.getMusteriDegeriPuan(Long customerId)` | Müşterinin şirket değeri puanı. |
| `CompanyService.findAll()` / `findById(Long)` / `getName(Long)` | Şirket sorguları. |
| `CompanyService.createCompany(String, MusteriDegeri)` / `updateCompany(...)` / `deleteCompany(Long)` | Şirket CRUD (silmede kullanıcı bağı koruması). |

### 3.5 `RequestMessageService` — Mesajlaşma
| Metot | Açıklama |
|---|---|
| `getMessages(Long requestId)` | Müşteri ↔ PO kanalı (dış, `internal=0`). |
| `getInternalMessages(Long requestId)` | Ekip kanalı (PO/SM/Geliştirici, `internal=1`; müşteri görmez). |
| `sendMessage(Long requestId, Long senderId, String body)` | Dış mesaj. Karşı tarafa bildirim. |
| `sendInternalMessage(Long requestId, Long senderId, String body)` | Ekip notu. PO+SM+atanmış geliştiriciye bildirim. |

### 3.6 `NotificationService` — Canlı bildirim
| Metot | Açıklama |
|---|---|
| `notify(Long userId, String message, Long requestId)` | Bildirim kaydeder ve canlı yayar (`@Push`). |
| `getUnreadCount(Long userId)` | Okunmamış sayısı. |
| `getRecent(Long userId)` | Son 20 bildirim. |
| `markAllRead(Long userId)` | Tümünü okundu işaretler. |
| `clearAll(Long userId)` | Tüm bildirimleri siler. |

Canlı yayın, `NotificationBroadcaster` (kullanıcı → dinleyici listesi) ve Vaadin `ui.access()`
ile yapılır. `NotificationBell` bileşeni her panelin kenar çubuğundadır.

### 3.7 `RequestFileService` — Talep ekleri
| Metot | Açıklama |
|---|---|
| `getFilesByRequestId(Long)` | Talebe ekli dosyalar (BLOB). |

---

## 4. Veritabanı Şeması

Tüm tablolar `Eren_` önekli. Birincil anahtarlar `Eren_seq_*` dizileriyle üretilir.
Zaman damgaları `SYSTIMESTAMP` ile atanır.

### `Eren_companies` — Şirketler
| Kolon | Tip | Açıklama |
|---|---|---|
| `company_id` | NUMBER (PK) | Birincil anahtar |
| `name` | VARCHAR2 | Şirket adı |
| `musteri_degeri` | VARCHAR2 | `VIP` / `BUYUK` / `ORTA` / `KUCUK` / `IC_KULLANICI` |
| `created_at`, `updated_at` | TIMESTAMP | |

### `Eren_users` — Kullanıcılar
| Kolon | Tip | Açıklama |
|---|---|---|
| `user_id` | NUMBER (PK) | |
| `name_surname` | VARCHAR2 | Ad soyad |
| `email` | VARCHAR2 (benzersiz) | Giriş kimliği |
| `password` | VARCHAR2 | BCrypt hash |
| `role` | VARCHAR2 | `CUSTOMER`/`PRODUCT_OWNER`/`DEVELOPER`/`SCRUM_MASTER`/`ADMIN` |
| `company_id` | NUMBER (FK → companies) | Müşteriler için şirket (diğerlerinde boş olabilir) |
| `active` | NUMBER(1) | 1=aktif, 0=pasif |
| `created_at`, `updated_at` | TIMESTAMP | |

### `Eren_requests` — Talepler
| Kolon | Tip | Açıklama |
|---|---|---|
| `request_id` | NUMBER (PK) | |
| `customer_id` | NUMBER (FK → users) | Talebi açan müşteri |
| `title` | VARCHAR2 | Başlık |
| `description` | VARCHAR2/CLOB | Detay |
| `status` | VARCHAR2 | `NEW`/`UNDER_REVIEW`/`PRIORITIZED`/`REJECTED`/`DUPLICATE` |
| `rejection_reason` | VARCHAR2 | Ret gerekçesi (boş olabilir) |
| `yonetici_takdiri` | VARCHAR2 | `YOK`/`ONEMLI`/`STRATEJIK`/`KRITIK` (boş olabilir) |
| `merged_into` | NUMBER (FK → requests) | Kopya birleştirmede ana talep (boş olabilir) |
| `created_at`, `updated_at` | TIMESTAMP | |

### `Eren_request_files` — Talep ekleri
| Kolon | Tip | Açıklama |
|---|---|---|
| `file_id` | NUMBER (PK) | |
| `request_id` | NUMBER (FK → requests) | |
| `file_name` | VARCHAR2 | Dosya adı |
| `file_data` | BLOB | İçerik |
| `file_size` | NUMBER | Bayt |
| `created_at` | TIMESTAMP | |

### `Eren_prioritizations` — Önceliklendirme
| Kolon | Tip | Açıklama |
|---|---|---|
| `priority_id` | NUMBER (PK) | |
| `request_id` | NUMBER (FK → requests) | |
| `is_etkisi` | NUMBER | İş etkisi (1–5) |
| `aciliyet` | NUMBER | Aciliyet (1–5) |
| `musteri_degeri_puan` | NUMBER | Şirket değeri puanı (1–5) |
| `is_tipi` | VARCHAR2 | `IsTipi` enum |
| `is_tipi_puan` | NUMBER | İş tipi puanı |
| `bekleme_suresi_puan` | NUMBER | Bekleme süresi puanı |
| `gelistirici_mudahalesi` | VARCHAR2 | Çaba tahmini (SM girene kadar boş) |
| `baz_skor` | NUMBER | Ara skor |
| `priority_score` | NUMBER | Nihai öncelik skoru |
| `created_at`, `updated_at` | TIMESTAMP | |

### `Eren_workflows` — İş akışı / görev
| Kolon | Tip | Açıklama |
|---|---|---|
| `task_id` | NUMBER (PK) | |
| `request_id` | NUMBER (FK → requests) | |
| `developer_id` | NUMBER (FK → users) | Atanan geliştirici (boş olabilir) |
| `workflow_status` | VARCHAR2 | `BACKLOG`/`IN_PROGRESS`/`TESTING`/`DONE` |
| `version` | NUMBER | İyimser kilit (optimistic locking) |
| `created_at`, `updated_at` | TIMESTAMP | |

### `Eren_request_messages` — Mesajlar
| Kolon | Tip | Açıklama |
|---|---|---|
| `message_id` | NUMBER (PK) | |
| `request_id` | NUMBER (FK → requests) | |
| `sender_id` | NUMBER (FK → users) | Gönderen |
| `body` | VARCHAR2(2000) | İçerik |
| `internal` | NUMBER(1) | 0=müşteri kanalı, 1=ekip kanalı |
| `created_at` | TIMESTAMP | |

### `Eren_notifications` — Bildirimler
| Kolon | Tip | Açıklama |
|---|---|---|
| `notification_id` | NUMBER (PK) | |
| `user_id` | NUMBER (FK → users) | Alıcı |
| `message` | VARCHAR2(500) | Metin |
| `request_id` | NUMBER | İlgili talep (tıklanınca gidilir; boş olabilir) |
| `is_read` | NUMBER(1) | 0=okunmadı, 1=okundu |
| `created_at` | TIMESTAMP | |

### İlişki Özeti
```
companies 1───* users 1───* requests 1───1 prioritizations
                    │             │
                    │             ├───1 workflows *───1 users (developer)
                    │             ├───* request_files
                    │             └───* request_messages *───1 users (sender)
                    └───* notifications
requests 0───1 requests (merged_into: kopya → ana talep)
```

---

## 5. Durum Makineleri

Geçişler [`StatusTransitionValidator`](src/main/java/com/example/request/StatusTransitionValidator.java)
tarafından zorunlu kılınır.

### Talep Durumu (`RequestStatus`)
```
NEW ──takeUnderReview──▶ UNDER_REVIEW ──markAsPrioritized──▶ PRIORITIZED
                              │                                   │
                              └──────rejectRequest──▶ REJECTED    │
                                                                  │
        (SM→PO geri gönderme) PRIORITIZED ──revertToUnderReview──▶ UNDER_REVIEW

NEW / UNDER_REVIEW / PRIORITIZED ──mergeDuplicate──▶ DUPLICATE (merged_into set edilir)
```

### İş Akışı Durumu (`WorkflowStatus`)
```
BACKLOG ──▶ IN_PROGRESS ──▶ TESTING ──▶ DONE      (ileri yön: updateStatus)

IN_PROGRESS / TESTING ──sendBackToScrumMaster──▶ BACKLOG   (geri gönderme)
BACKLOG ──sendBackToProductOwner──▶ (iş akışı silinir, talep UNDER_REVIEW'e döner)
```

---

## 6. Rol Bazlı Yetki Özeti

| Yetenek | CUSTOMER | PRODUCT_OWNER | SCRUM_MASTER | DEVELOPER | ADMIN |
|---|:---:|:---:|:---:|:---:|:---:|
| Talep oluştur / kendi taleplerini izle | ✅ | | | | |
| Talebi incele / önceliklendir / reddet | | ✅ | | | |
| Kopya tespit / birleştir | | ✅ | | | |
| İş akışına al | | ✅ | | | |
| Geliştirici ata / çaba tahmini | | | ✅ | | |
| Görev durumu ilerlet | | | | ✅ | |
| Geri gönderme | | | ✅ (→PO) | ✅ (→SM) | |
| Müşteri ile mesajlaşma | ✅ | ✅ | | | |
| Ekip içi not | | ✅ | ✅ | ✅ | |
| Kullanıcı / şirket yönetimi | | | | | ✅ |
| Canlı bildirim (zil) | ✅ | ✅ | ✅ | ✅ | |

---

*Bu belge kaynak koddan üretilmiştir. Route'lar `views/` altındaki `@Route` sınıflarından,
servis metotları `*Service` sınıflarından, şema ise `*Repository` sınıflarındaki SQL'lerden
çıkarılmıştır.*
