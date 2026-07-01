# Talep Yönetim Sistemi

Kurumsal bir **talep (destek/geliştirme) yönetim** uygulaması. Müşteriler talep oluşturur;
ürün sorumlusu (Product Owner) talepleri inceleyip önceliklendirir; scrum master görevleri
geliştiricilere atar; geliştiriciler iş akışını (Backlog → Devam → Test → Tamamlandı) yürütür.
Sistem otomatik **önceliklendirme skoru**, **kopya talep tespiti/birleştirme**, **talep-bazlı
mesajlaşma**, **ekip içi notlar**, **geri gönderme** ve **canlı bildirim** özellikleri içerir.

> **Mimari not:** Bu proje bir **Vaadin Flow** uygulamasıdır (sunucu-taraflı UI). Klasik REST
> API'si (HTTP uç noktaları) **yoktur**. Mimarinin, route'ların ve servis katmanının ayrıntılı
> açıklaması için [MIMARI.md](MIMARI.md) dosyasına bakın.

---

## Teknoloji Yığını

| Katman | Teknoloji |
|---|---|
| Dil | Java 25 |
| Uygulama çatısı | Spring Boot 4.0.7 |
| Arayüz (UI) | Vaadin Flow 25.1.8 (sunucu-taraflı) |
| Güvenlik | Spring Security (form login, rol bazlı) |
| Veri erişimi | Spring `JdbcTemplate` (JPA/Hibernate **yok**) |
| Veritabanı | Oracle Database (ojdbc11 + orai18n) |
| Yardımcı | Lombok, Vaadin `@Push` (canlı bildirim) |
| Derleme | Maven (Maven Wrapper `mvnw` ile) |

---

## Gereksinimler

- **JDK 25** (veya uyumlu bir sürüm)
- **Oracle Database** erişimi (mevcut yapılandırma: `192.168.10.2:1521:orcl`)
- İnternet (ilk derlemede Maven ve Vaadin frontend bağımlılıkları indirilir)
- Maven kurmaya gerek yok — depo içindeki `mvnw`/`mvnw.cmd` sarmalayıcısı kullanılır

---

## 1) Veritabanı Kurulumu

Uygulama JPA kullanmadığı için tabloları **otomatik oluşturmaz**. Şema, Oracle üzerinde elle
oluşturulmalıdır. Tüm nesneler `Eren_` önekiyle adlandırılır.

Gerekli **8 tablo** ve karşılık gelen **8 dizi (sequence)**:

| Tablo | Dizi (sequence) |
|---|---|
| `Eren_companies` | `Eren_seq_companies` |
| `Eren_users` | `Eren_seq_users` |
| `Eren_requests` | `Eren_seq_requests` |
| `Eren_request_files` | `Eren_seq_req_files` |
| `Eren_prioritizations` | `Eren_seq_prioritizations` |
| `Eren_workflows` | `Eren_seq_workflows` |
| `Eren_request_messages` | `Eren_seq_request_messages` |
| `Eren_notifications` | `Eren_seq_notifications` |

Her tablonun tam kolon listesi ve ilişkileri için [MIMARI.md → Veritabanı Şeması](MIMARI.md#veritabanı-şeması)
bölümüne bakın.

> **Önemli:** `src/main/resources/db/` altındaki migration `.sql` dosyaları şu an depoda mevcut
> değildir; tablolar Oracle üzerinde elle uygulanmıştır. Şemayı sıfırdan kurmanız gerekirse
> MIMARI.md'deki kolon tanımlarını temel alın.

### Bağlantı Yapılandırması

Bağlantı bilgileri [`src/main/resources/application.properties`](src/main/resources/application.properties)
içindedir:

```properties
spring.datasource.url=jdbc:oracle:thin:@192.168.10.2:1521:orcl?oracle.jdbc.defaultNChar=true
spring.datasource.username=stajdemo
spring.datasource.password=stajdemo
spring.datasource.driver-class-name=oracle.jdbc.OracleDriver
spring.datasource.hikari.connection-init-sql=ALTER SESSION SET NLS_LANGUAGE='TURKISH' NLS_TERRITORY='TURKEY'
```

Kendi veritabanınızı kullanacaksanız `url`, `username`, `password` alanlarını güncelleyin.

---

## 2) Uygulamayı Çalıştırma

Proje kök dizininde (`app/`):

```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

Varsayılan olarak **http://localhost:8080** adresinde açılır (`vaadin.launch-browser=true`
olduğu için tarayıcı otomatik açılır). Port değiştirmek için `PORT` ortam değişkenini kullanın
(veya `application.properties` içinde `server.port`).

İlk çalıştırmada Vaadin frontend'i derlenir (npm bağımlılıkları indirilir), bu birkaç dakika sürebilir.

### İlk Veri (Seed)

`Eren_users` tablosu **boşsa**, [`DataInitializer`](src/main/java/com/example/security/DataInitializer.java)
başlangıç şirketlerini ve kullanıcılarını otomatik oluşturur. Tabloda kullanıcı varsa bu adım atlanır.

---

## 3) Giriş Bilgileri (Seed Kullanıcılar)

| Rol | E-posta | Şifre |
|---|---|---|
| Admin | `admin@firma.com` | `Admin1234!` |
| Scrum Master | `ali.demir@firma.com` | `Sm1234!` |
| Ürün Sorumlusu (PO) | `ayse.kaya@firma.com` | `Po1234!` |
| Geliştirici | `mehmet.celik@firma.com` | `Dev1234!` |
| Geliştirici | `zeynep.arslan@firma.com` | `Dev1234!` |
| Müşteri (TeknoCorp) | `yetkili@teknocorp.com` | `Cust1234!` |
| Müşteri (Global A.Ş.) | `temsilci@globalas.com` | `Cust1234!` |
| Müşteri (Müşteri Ltd.) | `mehmet.oz@musteri.com` | `Cust1234!` |
| Müşteri (Medya TR) | `iletisim@medyatr.com` | `Cust1234!` |
| Müşteri (TeknoCorp 2.) | `destek@teknocorp.com` | `Cust1234!` |

Giriş sonrası kullanıcı, rolüne göre ilgili panele otomatik yönlendirilir (bkz. MIMARI.md).

---

## 4) Derleme / Paketleme

```bash
# Sadece derleme (testsiz)
.\mvnw.cmd -o compile -DskipTests

# Çalıştırılabilir jar üretimi (frontend derlemesi dahil)
.\mvnw.cmd package -DskipTests
java -jar target/app-1.0-SNAPSHOT.jar
```

### Geliştirici Notları

- **JDK 23+ ve Lombok:** JDK 23 ve üzeri, annotation processor'ları (Lombok) artık
  otomatik çalıştırmaz. Bu yüzden `pom.xml` içinde `maven-compiler-plugin`'e `<proc>full</proc>`
  eklenmiştir. Eksik olursa Lombok'un ürettiği getter/setter'lar bulunamaz ve derleme çöker.
- **`mvnw clean` çevrimdışı sorunu:** Bağımlılıklar önbellekte yoksa `clean` hedefi
  offline başarısız olabilir. Bu durumda `target/classes` klasörünü elle silip yeniden derleyin.

---

## Proje Yapısı

```
src/main/java/com/example/
├── Application.java            # Spring Boot giriş noktası (@Push burada)
├── enums/                      # Role, RequestStatus, WorkflowStatus, IsTipi, MusteriDegeri, ...
├── company/                    # Şirket (Company) — model, repository, service
├── user/                       # Kullanıcı (User) — model, repository, service
├── request/                    # Talep + talep dosyaları + durum geçiş doğrulayıcı
├── prioritization/             # Önceliklendirme skoru (model, repository, service)
├── workflow/                   # İş akışı / görev (Backlog→...→Done)
├── message/                    # Talep-bazlı mesajlaşma (müşteri kanalı + ekip kanalı)
├── notification/               # Canlı bildirim (repository, service, broadcaster, zil bileşeni)
├── security/                   # Spring Security yapılandırması + DataInitializer
├── util/                       # DateUtil, GridSearch yardımcıları
└── views/                      # Vaadin ekranları (rol bazlı paneller + login)

src/main/resources/
├── application.properties      # Sunucu + Oracle bağlantı ayarları
└── META-INF/resources/styles.css
```

Ayrıntılı mimari, katman sorumlulukları, servis "API"si ve veritabanı şeması için
**[MIMARI.md](MIMARI.md)** dosyasına bakın.
