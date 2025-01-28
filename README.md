# Paddle & Tennis Booking App

Un'applicazione mobile sviluppata come progetto per l'esame di **Programmazione Mobile**. L'obiettivo principale è semplificare la gestione delle prenotazioni di campi sportivi e lezioni private per centri dedicati al paddle e al tennis.

## Tecnologie Utilizzate
- **Linguaggio:** Kotlin
- **IDE:** Android Studio
- **Database:** Firebase Realtime Database
- **Autenticazione:** Firebase Authentication

---

## Funzionalità Utente

### Autenticazione
Gli utenti possono registrarsi fornendo:
- Nome e cognome
- Data di nascita
- Genere
- Email
- Password

Dopo la registrazione, è possibile accedere alla propria area riservata.

### Sezioni principali
L'app è suddivisa in 5 sezioni:

1. **Profile**
   - Visualizzazione di bio, icona di profilo standard e reputazione (media voti da 0 a 5).
   - Modifica dei dati personali.
   - Logout.

2. **Lesson**
   - Prenotazione di lezioni private con i maestri disponibili.
   - Scelta di uno slot orario.
   - Conferma della prenotazione.

3. **Booking**
   - Prenotazione di campi per partite (fino a 4 partecipanti).
   - Aggiunta di partecipanti tramite barra di ricerca.
   - Modifica e cancellazione delle prenotazioni.

4. **Search**
   - Ricerca e visualizzazione di altri utenti registrati.
   - Funzioni per aggiungere/rimuovere amicizie, valutare altri utenti e segnalare comportamenti scorretti.

5. **Home**
   - Panoramica delle prenotazioni attive (campi e lezioni).
   - Modifica o eliminazione delle prenotazioni.

---

## Funzionalità Admin
Gli utenti con privilegi di amministrazione hanno accesso a:
- **Lesson e Booking:** aggiunta di nuovi campi, maestri e slot disponibili.
- **Search:** gestione dei report ricevuti dagli utenti, con opzioni per bannare/sbannare.

---

## Struttura del Database Firebase
1. **Coaches:** elenco dei maestri e relativi slot orari.
2. **Fields:** informazioni sui campi disponibili.
3. **Reservations:** gestione delle prenotazioni (campi e lezioni).
4. **Users:** dati personali, amicizie, reputazioni e segnalazioni.

---

## Test e Compatibilità
L'app è stata testata su:
- **Device reale:** Pixel 8 Pro
- **Device virtuali:** Pixel 7, Pixel 4

### Utenti di test
- **Admin:**  
  Email: `giulia.bianchi99@gmail.com`  
  Password: `giulia99`
- **Utente base:**  
  Email: `marco.neri99@gmail.com`  
  Password: `marco99`

---

## Considerazioni Finali
Questo progetto combina funzionalità pratiche per gli utenti e strumenti di gestione avanzati per gli amministratori. Lo sviluppo in **Kotlin** con **Firebase** ha rappresentato un'importante esperienza di apprendimento, consolidando competenze tecniche e di progettazione software.

---

## Contatti
Autore: **Gabriele Privitera**  
[GitHub Profile](https://github.com/PriviteraGabriele)  
[Email](mailto:gabriele.pr.01@gmail.com)
