.
# Habit Flow

App Android nativa per routine, tracking, statistiche e modalità Studente, costruita con Kotlin, Jetpack Compose, Room, DataStore, AlarmManager e notifiche full-screen.

## Requisiti
- Android Studio Ladybug o successiva
- JDK 17 o JDK 21
- Android SDK API 35
- Smartphone Android 8.0 (API 26) o superiore

## Esecuzione con Android Studio
1. Apri Android Studio.
2. Seleziona **File > Open** e scegli questa cartella.
3. Attendi la sincronizzazione Gradle e installa, se richiesti, SDK 35 / Build Tools.
4. Collega lo smartphone via USB con la modalità sviluppatore e il debug USB attivi.
5. Seleziona il dispositivo dal menu Run/Device Manager.
6. Premi **Run 'app'**.

## Terminale
```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Permessi
Al primo avvio abilita nelle Impostazioni dell'app: notifiche, sveglia esatta, intent a schermo intero ed esclusione dall'ottimizzazione della batteria.

## Funzioni
- Giorno: calendario mensile, routine e tracking rapido
- Routine: creazione, giorni, orario, subazioni e allarme esatto
- Tracking: frequenza giornaliera, mensile, settimanale o timer
- Griglia: quadratini da 7 colonne per il mese reale
- Studente: esami, CFU, date d'appello, argomenti e timer Pomodoro
- Statistiche: categorie e andamento delle attività
- Drawer: categorie, tema, palette e modalità Studente
