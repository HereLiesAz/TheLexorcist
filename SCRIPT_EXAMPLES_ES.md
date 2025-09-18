# 60 ejemplos de scripts para The Lexorcist

Este documento proporciona 60 ejemplos de scripts únicos para el creador de scripts de The Lexorcist. Estos scripts están diseñados para mostrar el poder y la flexibilidad del sistema, comenzando desde el etiquetado de palabras clave simple y progresando hacia funciones más complejas, analíticas e incluso orientadas a la gestión de casos.

A los efectos de estos ejemplos, asumimos que el entorno de secuencias de comandos proporciona la siguiente API:

*   `evidence`: Un objeto que contiene la pieza de evidencia actual que se está procesando.
    *   `evidence.text`: El contenido de texto de la evidencia (de OCR o transcripción).
    *   `evidence.metadata`: Un objeto con metadatos como `date`, `source`, etc.
*   `case`: Un objeto que representa todo el caso.
    *   `case.allegations`: Una lista de alegaciones para el caso.
    *   `case.evidence`: Una lista de todas las demás pruebas del caso.
*   `addTag(tagName)`: Una función para agregar una etiqueta a la pieza de evidencia actual.
*   `setSeverity(level)`: Una función para establecer un nivel de gravedad (p. ej., "Bajo", "Medio", "Alto", "Crítico").
*   `linkToAllegation(allegationName)`: Una función para vincular la evidencia a una alegación específica.
*   `createNote(noteText)`: Una función para agregar una nota u observación a la evidencia.

---

### **Nivel 1: Etiquetado y extracción básicos**

**1. Etiquetador de blasfemias**
*   **Descripción:** Un script simple para etiquetar cualquier evidencia que contenga malas palabras comunes. Útil para establecer un tono general de comunicación.
*   **Script:**
    ```javascript
    const curses = ["joder", "mierda", "puta", "gilipollas"]; // Agregue más según sea necesario
    const text = evidence.text.toLowerCase();
    if (curses.some(word => text.includes(word))) {
        addTag("Profanity");
    }
    ```

**2. Etiquetador de menciones financieras**
*   **Descripción:** Identifica y etiqueta cualquier mención de dinero o transacciones financieras.
*   **Script:**
    ```javascript
    const financialRegex = /[$€£¥]|\b(dólar|euro|yen|libra|pago|pagado|deber|dinero)\b/i;
    if (financialRegex.test(evidence.text)) {
        addTag("Financial");
    }
    ```

**3. Extractor de información de contacto**
*   **Descripción:** Encuentra y etiqueta números de teléfono y direcciones de correo electrónico, lo que facilita la búsqueda de datos de contacto más adelante.
*   **Script:**
    ```javascript
    const emailRegex = /\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b/i;
    const phoneRegex = /\b\d{3}[-.]?\d{3}[-.]?\d{4}\b/;
    if (emailRegex.test(evidence.text)) {
        addTag("Email Address");
    }
    if (phoneRegex.test(evidence.text)) {
        addTag("Phone Number");
    }
    ```

**4. Etiquetador de citas y plazos**
*   **Descripción:** Busca menciones de fechas, citas o plazos específicos.
*   **Script:**
    ```javascript
    const deadlineRegex = /\b(cita|reunión|plazo|vence el|fecha del tribunal)\b/i;
    if (deadlineRegex.test(evidence.text)) {
        addTag("Deadline/Appointment");
    }
    ```

---

### **Nivel 2: Reconocimiento de patrones y análisis simple**

**5. Detector de gaslighting**
*   **Descripción:** Identifica frases de uso común en el gaslighting para negar la realidad de una persona.
*   **Script:**
    ```javascript
    const gaslightingPhrases = ["estás loco", "eso nunca pasó", "estás imaginando cosas", "eres demasiado sensible", "nunca dije eso"];
    const text = evidence.text.toLowerCase();
    if (gaslightingPhrases.some(phrase => text.includes(phrase))) {
        addTag("Gaslighting");
        linkToAllegation("Emotional Abuse");
    }
    ```

**6. Evaluador de la gravedad de la amenaza**
*   **Descripción:** Un detector de amenazas más matizado que asigna un nivel de gravedad según el tipo de amenaza.
*   **Script:**
    ```javascript
    const lowSeverity = ["te haré pagar"];
    const medSeverity = ["arruinaré tu vida"];
    const highSeverity = ["te voy a hacer daño", "te mataré"];
    const text = evidence.text.toLowerCase();

    if (highSeverity.some(p => text.includes(p))) {
        addTag("Threat");
        setSeverity("Critical");
    } else if (medSeverity.some(p => text.includes(p))) {
        addTag("Threat");
        setSeverity("High");
    } else if (lowSeverity.some(p => text.includes(p))) {
        addTag("Threat");
        setSeverity("Medium");
    }
    ```

**7. Etiquetador de violación de la custodia**
*   **Descripción:** Específicamente para casos de derecho de familia, este script busca violaciones de los acuerdos de custodia.
*   **Script:**
    ```javascript
    const violationPhrases = ["no puedes ver a los niños", "no lo voy a traer de vuelta", "no es tu fin de semana"];
    if (violationPhrases.some(p => evidence.text.toLowerCase().includes(p))) {
        addTag("Custody Violation");
        linkToAllegation("Child Custody Interference");
    }
    ```

**8. Detector de admisión de culpa**
*   **Descripción:** Marca frases que podrían interpretarse como una admisión de irregularidades.
*   **Script:**
    ```javascript
    const admissionPhrases = ["sé que la cagué", "fue mi culpa", "lamento haber hecho eso", "no debería haberlo hecho"];
    if (admissionPhrases.some(p => evidence.text.toLowerCase().includes(p))) {
        addTag("Admission");
    }
    ```

---

### **Nivel 3: Análisis de evidencia cruzada**

**9. Detector de patrones de acecho**
*   **Descripción:** Este script busca patrones de acecho correlacionando menciones de ubicación en múltiples pruebas.
*   **Script:**
    ```javascript
    const locationRegex = /te vi en (.+?)\b/i;
    const match = evidence.text.match(locationRegex);
    if (match) {
        const location = match[1];
        addTag(`Location Mention: ${location}`);
        const otherMentions = case.evidence.filter(e =>
            e.tags.some(t => t.startsWith("Location Mention:"))
        );
        if (otherMentions.length > 2) {
            addTag("Stalking Pattern");
            linkToAllegation("Stalking");
            createNote(`Múltiples menciones de ubicación no invitadas detectadas. Comportamiento potencial de acecho.`);
        }
    }
    ```

**10. Buscador de contradicciones**
*   **Descripción:** Analiza otras pruebas para encontrar contradicciones directas.
*   **Script:**
    ```javascript
    const promiseRegex = /prometo (pagarte|darte)(.+?)\b/i;
    const denialRegex = /nunca prometí (pagarte|darte)(.+?)\b/i;

    const promiseMatch = evidence.text.match(promiseRegex);
    if (promiseMatch) {
        const promisedItem = promiseMatch[2].trim();
        const contradiction = case.evidence.find(e => {
            const denialMatch = e.text.match(denialRegex);
            return denialMatch && denialMatch[2].trim() === promisedItem;
        });
        if (contradiction) {
            addTag("Contradiction");
            createNote(`Esto contradice la evidencia de ${contradiction.metadata.date}.`);
        }
    }
    ```

**11. Rastreador de escalada**
*   **Descripción:** Rastrea la gravedad de las amenazas a lo largo del tiempo para mostrar un patrón de escalada.
*   **Script:**
    ```javascript
    if (evidence.tags.includes("Threat")) {
        const pastThreats = case.evidence.filter(e =>
            e.tags.includes("Threat") && new Date(e.metadata.date) < new Date(evidence.metadata.date)
        );
        const pastSeverity = pastThreats.map(e => e.metadata.severity);
        if (pastSeverity.includes("Medium") && evidence.metadata.severity === "High") {
            addTag("Escalating Behavior");
            createNote("La gravedad ha aumentado de Media a Alta.");
        }
    }
    ```

**12. Corroborador/interruptor de coartada**
*   **Descripción:** Comprueba si hay consistencias o inconsistencias en las coartadas.
*   **Script:**
    ```javascript
    const alibiRegex = /estaba en (.+?) a las (\d{1,2}:\d{2}[ap]m)/i;
    const match = evidence.text.match(alibiRegex);
    if (match) {
        const location = match[1];
        const time = match[2];
        addTag(`Alibi: ${location} at ${time}`);
        const conflictingEvidence = case.evidence.find(e =>
            e.text.includes(`te vi en un lugar diferente a las ${time}`)
        );
        if (conflictingEvidence) {
            addTag("Alibi Conflict");
        }
    }
    ```

---

### **Nivel 4: Análisis avanzado y gestión de casos**

**13. Identificador de testigos de terceros**
*   **Descripción:** Busca menciones de otras personas que podrían ser testigos potenciales.
*   **Script:**
    ```javascript
    const witnessRegex = /\b(pregúntale a|habla con|con|vio|dijo) ([A-Z][a-z]+)\b/g;
    let match;
    while ((match = witnessRegex.exec(evidence.text)) !== null) {
        const witnessName = match[2];
        if (witnessName.toLowerCase() !== "tú") {
            addTag(`Potential Witness: ${witnessName}`);
        }
    }
    ```

**14. Generador de solicitudes de documentos**
*   **Descripción:** Identifica menciones de documentos que podría necesitar solicitar en el descubrimiento.
*   **Script:**
    ```javascript
    const docRegex = /\b(contrato|acuerdo|arrendamiento|recibo|factura|extracto bancario)\b/ig;
    let match;
    while ((match = docRegex.exec(evidence.text)) !== null) {
        addTag("Mentioned Document");
        createNote(`Sugerencia de descubrimiento: solicite el '${match[0]}' mencionado aquí.`);
    }
    ```

**15. Detector de apagón de comunicación**
*   **Descripción:** Identifica brechas repentinas y prolongadas en la comunicación, lo que puede ser significativo.
*   **Script:**
    ```javascript
    const lastCommunication = case.evidence
        .filter(e => e.metadata.source === evidence.metadata.source)
        .sort((a, b) => new Date(b.metadata.date) - new Date(a.metadata.date))[1]; // Obtenga el anterior a este

    if (lastCommunication) {
        const daysSince = (new Date(evidence.metadata.date) - new Date(lastCommunication.metadata.date)) / (1000 * 3600 * 24);
        if (daysSince > 14) { // Brecha de 2 semanas
            addTag("Communication Gap");
            createNote(`Se produjo una brecha de comunicación de ${Math.round(daysSince)} días antes de este mensaje.`);
        }
    }
    ```

**16. Analizador de coerción "Si esto, entonces aquello"**
*   **Descripción:** Un script más avanzado para detectar declaraciones coercitivas de "si-entonces".
*   **Script:**
    ```javascript
    const coercionRegex = /si (no|no quieres) (.+?), (entonces )?lo haré (.+)/i;
    const match = evidence.text.match(coercionRegex);
    if (match) {
        const demand = match[2];
        const consequence = match[4];
        addTag("Coercion");
        linkToAllegation("Coercion/Blackmail");
        createNote(`Declaración coercitiva detectada. Demanda: '${demand}', Consecuencia: '${consequence}'.`);
    }
    ```

---

### **Nivel 5: Scripts generativos y con tecnología de inteligencia artificial (conceptuales)**

*Estos scripts pueden requerir capacidades más avanzadas que el simple JS, como bibliotecas hipotéticas de IA/ML disponibles en el entorno de scripts.*

**17. Análisis de cambio de sentimiento**
*   **Descripción:** Utiliza una biblioteca de análisis de sentimientos hipotética para rastrear el tono emocional a lo largo del tiempo y marca los cambios drásticos.
*   **Script:**
    ```javascript
    // Asume una biblioteca de Sentimiento hipotética
    const currentSentiment = Sentiment.analyze(evidence.text).score; // p. ej., -0.8 (muy negativo)
    const recentEvidence = case.evidence.slice(-5);
    const avgPastSentiment = recentEvidence.reduce((acc, e) => acc + Sentiment.analyze(e.text).score, 0) / recentEvidence.length;

    if (Math.abs(currentSentiment - avgPastSentiment) > 1.5) { // Detecta un cambio importante
        addTag("Sentiment Shift");
        createNote(`Cambio drástico de sentimiento de un promedio de ${avgPastSentiment.toFixed(2)} a ${currentSentiment.toFixed(2)}.`);
    }
    ```

**18. Mapeador de elementos legales**
*   **Descripción:** Intenta mapear la evidencia con los elementos legales específicos de una alegación. Por ejemplo, para "Incumplimiento de contrato", busca evidencia de una oferta, aceptación y un incumplimiento.
*   **Script:**
    ```javascript
    if (case.allegations.includes("Breach of Contract")) {
        if (evidence.text.toLowerCase().includes("acepto tu oferta")) {
            addTag("Contract - Acceptance");
        }
        if (evidence.text.toLowerCase().includes("no se entregó")) {
            addTag("Contract - Breach");
        }
    }
    ```

**19. Sugeridor de brechas de evidencia**
*   **Descripción:** Un script inteligente que sugiere qué evidencia podría faltar.
*   **Script:**
    ```javascript
    if (evidence.tags.includes("Mentioned Document")) {
        const docName = evidence.text.match(/\b(contrato|acuerdo|etc)\b/i)[0];
        const hasDocument = case.evidence.some(e => e.metadata.type === 'document' && e.metadata.title.includes(docName));
        if (!hasDocument) {
            createNote(`Evidencia faltante: el '${docName}' mencionado aquí no se ha agregado al caso.`);
        }
    }
    ```

**20. Generador de resúmenes narrativos**
*   **Descripción:** El script definitivo. Para una alegación dada, extrae toda la evidencia vinculada e intenta crear un resumen cronológico de los eventos.
*   **Script:**
    ```javascript
    // Esto es muy conceptual
    if (case.allegations.includes("Harassment")) {
        const harassmentEvidence = case.evidence
            .filter(e => e.allegations.includes("Harassment"))
            .sort((a, b) => new Date(a.metadata.date) - new Date(a.metadata.date));

        let summary = "Resumen de acoso:\n";
        harassmentEvidence.forEach(e => {
            summary += `- El ${e.metadata.date}, se recibió un mensaje que contenía '${e.tags.join(', ')}'.\n`;
        });

        // En un escenario real, esto se guardaría en un archivo de resumen de caso o en una nota.
        // Por ahora, agregaremos una nota a la última pieza de evidencia.
        if (evidence.id === harassmentEvidence.slice(-1)[0].id) {
            createNote(summary);
        }
    }
    ```

---

### **Nivel 6: Análisis avanzado de integridad de casos y pruebas**

**21. Verificador de la cadena de custodia**
*   **Descripción:** Para las pruebas que han sido manejadas por varias personas, este script comprueba si existe un registro completo de la cadena de custodia en las notas o metadatos de las pruebas. Marca cualquier prueba con lagunas.
*   **Script:**
    ```javascript
    // Asume un campo de metadatos `evidence.metadata.custodyLog` que es una matriz de registros de transferencia.
    if (evidence.metadata.type === 'physical') {
        const log = evidence.metadata.custodyLog;
        if (!log || log.length === 0) {
            addTag("Missing Chain of Custody");
            createNote("Crítico: a la evidencia física le falta su registro de cadena de custodia.");
            return;
        }
        for (let i = 0; i < log.length - 1; i++) {
            if (log[i].toPerson !== log[i+1].fromPerson) {
                addTag("Broken Chain of Custody");
                createNote(`Cadena de custodia rota entre ${log[i].toPerson} y ${log[i+1].fromPerson}.`);
                setSeverity("Critical");
            }
        }
    }
    ```

**22. Detector de anomalías de metadatos**
*   **Descripción:** Busca metadatos sospechosos, como los datos EXIF de una foto que muestran una fecha que contradice la fecha proporcionada por el usuario, o un archivo que ha sido modificado después de que supuestamente fue recopilado.
*   **Script:**
    ```javascript
    // Asume que los datos EXIF se analizan en `evidence.metadata.exif`
    if (evidence.metadata.exif && evidence.metadata.exif.dateCreated) {
        const exifDate = new Date(evidence.metadata.exif.dateCreated);
        const providedDate = new Date(evidence.metadata.date);
        const diffDays = Math.abs(exifDate - providedDate) / (1000 * 3600 * 24);
        if (diffDays > 1) {
            addTag("Metadata Anomaly");
            createNote(`La fecha de creación de EXIF (${exifDate.toDateString()}) difiere de la fecha proporcionada (${providedDate.toDateString()}).`);
            setSeverity("High");
        }
    }
    ```

**23. Buscador de pruebas duplicadas**
*   **Descripción:** Utiliza un hash perceptual (phash) o una simple suma de verificación almacenada en los metadatos para encontrar imágenes duplicadas o casi duplicadas, incluso si tienen nombres de archivo diferentes.
*   **Script:**
    ```javascript
    // Asume un campo de metadatos `evidence.metadata.phash`
    if (evidence.metadata.phash) {
        const duplicates = case.evidence.filter(e =>
            e.id !== evidence.id && e.metadata.phash === evidence.metadata.phash
        );
        if (duplicates.length > 0) {
            addTag("Duplicate");
            createNote(`Esto es un duplicado de la evidencia: ${duplicates.map(d => d.id).join(', ')}.`);
        }
    }
    ```

**24. Analizador de estilo de comunicación**
*   **Descripción:** Analiza el estilo lingüístico (p. ej., longitud de la oración, elección de palabras, uso de emojis) de una prueba y lo compara con la línea de base conocida del autor. Marca los mensajes que pueden haber sido escritos por otra persona.
*   **Script:**
    ```javascript
    // Conceptual: requiere un perfil de referencia para el autor.
    // `profile.authors['JohnDoe'].avgSentenceLength`
    const sentences = evidence.text.split(/[.!?]+/);
    const avgLength = evidence.text.length / sentences.length;
    const authorProfile = profile.authors[evidence.metadata.author];
    if (authorProfile && Math.abs(avgLength - authorProfile.avgSentenceLength) > 10) {
        addTag("Atypical Style");
        createNote("El estilo de comunicación es inconsistente con la línea de base del autor.");
    }
    ```

---

### **Nivel 7: Tareas dinámicas y automatización del flujo de trabajo**

**25. Generador automatizado de preguntas de deposición**
*   **Descripción:** Cuando encuentra una contradicción o una amenaza, este script genera automáticamente una posible pregunta de deposición y la agrega a una nota de "Preparación de deposición" para todo el caso.
*   **Script:**
    ```javascript
    function addDepoQuestion(question) {
        // Esta función se agregaría a una nota de caso central o a una lista de tareas.
        console.log(`Nueva pregunta de deposición: ${pregunta}`);
    }

    if (evidence.tags.includes("Contradiction")) {
        addDepoQuestion(`El [Fecha], usted declaró X. Sin embargo, el [Otra fecha], usted declaró Y. ¿Puede explicar esta discrepancia?`);
    }
    if (evidence.tags.includes("Threat")) {
        addDepoQuestion(`¿Puede explicar a qué se refería cuando escribió: "${evidence.text}"?`);
    }
    ```

**26. Recomendador de peritos**
*   **Descripción:** En función del contenido de la prueba, este script sugiere cuándo podría ser necesario un perito.
*   **Script:**
    ```javascript
    const financialRegex = /\b(contabilidad forense|fraude fiscal|malversación)\b/i;
    const techRegex = /\b(cifrado|piratería|dirección IP|metadatos)\b/i;

    if (financialRegex.test(evidence.text)) {
        addTag("Expert Witness Needed");
        createNote("Considere la posibilidad de contratar a un contador forense para esta evidencia.");
    }
    if (techRegex.test(evidence.text)) {
        addTag("Expert Witness Needed");
        createNote("Considere la posibilidad de contratar a un experto en informática forense para esta evidencia.");
    }
    ```

**27. Sugeridor automático de redacción**
*   **Descripción:** Identifica información de identificación personal (PII), como números de seguro social, números de cuentas bancarias o direcciones particulares, y los etiqueta para su redacción antes de compartirlos.
*   **Script:**
    ```javascript
    const ssnRegex = /\b\d{3}-\d{2}-\d{4}\b/;
    const bankAccountRegex = /\b\d{10,16}\b/;
    if (ssnRegex.test(evidence.text) || bankAccountRegex.test(evidence.text)) {
        addTag("Redact PII");
        setSeverity("High");
    }
    ```

**28. Analizador de ofertas de liquidación**
*   **Descripción:** Cuando se menciona una oferta de liquidación, este script rastrea el caso en busca de todas las pruebas etiquetadas como "Financieras" y calcula un total acumulado de los daños reclamados para proporcionar un contexto para la oferta.
*   **Script:**
    ```javascript
    const offerRegex = /te ofreceré \$([\d,.]+)/i;
    const match = evidence.text.match(offerRegex);
    if (match) {
        const offerAmount = parseFloat(match[1].replace(/,/g, ''));
        addTag("Settlement Offer");

        let totalDamages = 0;
        const financialEvidence = case.evidence.filter(e => e.tags.includes("Financial"));
        financialEvidence.forEach(e => {
            const damageMatch = e.text.match(/me debes \$([\d,.]+)/i);
            if (damageMatch) {
                totalDamages += parseFloat(damageMatch[1].replace(/,/g, ''));
            }
        });

        createNote(`Oferta de liquidación de $${offerAmount} recibida. Daños totales calculados en el caso: $${totalDamages}.`);
    }
    ```

---

### **Nivel 8: Análisis estratégico y de varios casos**

**29. Vinculador de actores entre casos**
*   **Descripción:** Identifica si un actor (p. ej., una persona, una empresa) en este caso ha aparecido en algún otro caso en su base de datos. Requiere una API de base de datos de casos global.
*   **Script:**
    ```javascript
    // Conceptual: requiere un objeto `database` global.
    const actorName = evidence.metadata.author;
    const otherCases = database.findCasesByActor(actorName);
    if (otherCases.length > 0) {
        addTag("Cross-Case Link");
        createNote(`El actor ${actorName} también aparece en los casos: ${otherCases.map(c => c.name).join(', ')}.`);
    }
    ```

**30. Sugeridor de precedentes legales**
*   **Descripción:** Identifica frases y conceptos clave en la evidencia y sugiere buscar precedentes legales relacionados con ellos.
*   **Script:**
    ```javascript
    // Conceptual: requiere una API de investigación legal.
    if (evidence.tags.includes("Gaslighting") && evidence.tags.includes("Financial")) {
        createNote("Sugerencia de investigación: busque precedentes sobre 'abuso económico' y 'control coercitivo'.");
        // En una versión más avanzada:
        // LegalResearchAPI.findPrecedents({topic: "economic abuse"});
    }
    ```

**31. Identificador de "prueba irrefutable"**
*   **Descripción:** Este script combina múltiples indicadores de alto valor para marcar una prueba como una posible "prueba irrefutable".
*   **Script:**
    ```javascript
    let score = 0;
    if (evidence.tags.includes("Admission")) score++;
    if (evidence.tags.includes("Contradiction")) score++;
    if (evidence.metadata.severity === "Critical") score++;
    if (evidence.tags.includes("Atypical Style")) score++; // Sugiere que alguien intenta ocultar algo

    if (score >= 3) {
        addTag("Smoking Gun?");
        setSeverity("Critical");
        createNote("Esta evidencia tiene múltiples indicadores de alto valor. Priorizar para su revisión.");
    }
    ```

**32. Barómetro de la solidez del caso**
*   **Descripción:** Un meta-script que se ejecuta periódicamente sobre todo el caso, evaluando la proporción de evidencia vinculada a alegaciones versus evidencia no vinculada, y el número de elementos "Prueba irrefutable" o "Críticos", para proporcionar un barómetro aproximado de la solidez del caso.
*   **Script:**
    ```javascript
    // Es probable que este sea un script independiente que se ejecute en el objeto del caso, no en una sola pieza de evidencia.
    const linkedEvidence = case.evidence.filter(e => e.allegations.length > 0).length;
    const totalEvidence = case.evidence.length;
    const strengthRatio = linkedEvidence / totalEvidence;
    const criticalItems = case.evidence.filter(e => e.metadata.severity === "Critical").length;

    let strength = "Débil";
    if (strengthRatio > 0.5) strength = "Moderado";
    if (strengthRatio > 0.75) strength = "Fuerte";
    if (criticalItems > 2) strength = "Muy fuerte";

    createNote(`Barómetro de solidez del caso: ${strength} (Proporción: ${strengthRatio.toFixed(2)}, Elementos críticos: ${criticalItems})`);
    ```

---

### **Nivel 9: IA generativa y predictiva (conceptual)**

**33. Predictor de la estrategia del abogado contrario**
*   **Descripción:** En función de las pruebas que tiene, este script intenta predecir la probable estrategia de defensa del abogado contrario.
*   **Script:**
    ```javascript
    // IA conceptual
    const hasGaslighting = case.evidence.some(e => e.tags.includes("Gaslighting"));
    const hasAdmissions = case.evidence.some(e => e.tags.includes("Admission"));

    if (hasGaslighting && !hasAdmissions) {
        createNote("Defensa prevista: es probable que el abogado contrario argumente que la evidencia es fabricada o que el cliente no es confiable (defensa 'inestable').");
    } else if (hasAdmissions) {
        createNote("Defensa prevista: el abogado contrario puede intentar una defensa de 'remordimiento' o argumentar que las admisiones se sacaron de contexto.");
    }
    ```

**34. Detector de eslabones narrativos perdidos**
*   **Descripción:** Analiza la cronología de los acontecimientos y señala las lagunas lógicas en las que debería existir evidencia pero no existe.
*   **Script:**
    ```javascript
    // IA conceptual
    const threat = case.evidence.find(e => e.tags.includes("Threat"));
    if (threat) {
        const followUp = case.evidence.find(e => new Date(e.metadata.date) > new Date(threat.metadata.date));
        if (!followUp) {
            createNote("Brecha narrativa: se hizo una amenaza, pero no hay evidencia posterior que muestre el resultado o la desescalada. ¿Qué pasó después?");
        }
    }
    ```

**35. Evaluador de riesgo de falsificación de pruebas**
*   **Descripción:** Utiliza un modelo de IA conceptual para analizar una imagen en busca de signos de manipulación digital (p. ej., niveles de compresión inconsistentes, anomalías de píxeles).
*   **Script:**
    ```javascript
    // IA conceptual
    if (evidence.metadata.type === 'image') {
        const forgeryRisk = ForgeryDetectionAI.analyze(evidence.imagePath); // devuelve una puntuación de 0 a 1
        if (forgeryRisk > 0.8) {
            addTag("Forgery Risk");
            setSeverity("Critical");
            createNote(`El análisis de IA indica un ${forgeryRisk*100}% de riesgo de manipulación digital.`);
        }
    }
    ```

**36. Investigación automatizada de testigos**
*   **Descripción:** Cuando se identifica a un testigo potencial, este script podría (con permiso) ejecutar una API de verificación de antecedentes conceptual para buscar registros públicos, conflictos de intereses o casos pasados de perjurio.
*   **Script:**
    ```javascript
    // API conceptual
    if (evidence.tags.some(t => t.startsWith("Potential Witness:"))) {
        const witnessName = evidence.tags.find(t => t.startsWith("Potential Witness:")).split(": ")[1];
        const backgroundCheck = BackgroundCheckAPI.run(witnessName);
        if (backgroundCheck.hasRedFlags) {
            createNote(`Investigación de testigos: ${witnessName} tiene posibles señales de alerta: ${backgroundCheck.flags.join(', ')}.`);
        }
    }
    ```

---

### **Nivel 10: Gestión de casos totalmente autónoma (conceptual)**

**37. Auto-categorizar y archivar evidencia**
*   **Descripción:** Un script impulsado por IA que lee la evidencia y la archiva automáticamente bajo la alegación más relevante sin necesidad de reglas explícitas.
*   **Script:**
    ```javascript
    // IA conceptual
    const textToClassify = evidence.text;
    const allAllegations = case.allegations;
    const bestFitAllegation = ClassificationAI.findBestMatch(textToClassify, allAllegations);
    if (bestFitAllegation.confidence > 0.7) {
        linkToAllegation(bestFitAllegation.name);
    }
    ```

**38. Redactor dinámico de solicitudes de descubrimiento**
*   **Descripción:** Va más allá de sugerir solicitudes. Este script redactaría un documento formal de solicitud de descubrimiento basado en las brechas que ha identificado.
*   **Script:**
    ```javascript
    // Generación de documentos conceptuales
    if (evidence.tags.includes("Missing Evidence")) {
        const missingDoc = evidence.notes.find(n => n.startsWith("Missing Evidence:")).split("'")[1];
        const requestDraft = DocumentGenerator.create('DiscoveryRequest', {
            itemNumber: 1,
            description: `Todos los documentos relacionados con el '${missingDoc}' mencionado en la comunicación con fecha ${evidence.metadata.date}.`
        });
        // guardar requestDraft en los archivos del caso
    }
    ```

**39. Predictor del resultado del caso**
*   **Descripción:** El santo grial. Una IA conceptual que analiza todas las pruebas, la solidez del caso, los precedentes vinculados y la estrategia del abogado contrario para proporcionar una predicción probabilística del resultado.
*   **Script:**
    ```javascript
    // IA conceptual
    const prediction = OutcomePredictionAI.analyze(case);
    // prediction = { outcome: "Acuerdo favorable", confidence: 0.65, keyFactors: ["Prueba irrefutable", "Sólida lista de testigos"] }
    createNote(`Predicción del resultado: ${prediction.outcome} (Confianza: ${prediction.confidence*100}%) basado en: ${prediction.keyFactors.join(', ')}.`);
    ```

**40. Generación automatizada de resúmenes de casos y escritos**
*   **Descripción:** Un script generativo final que toma los resúmenes narrativos, las pruebas clave, las listas de testigos y los precedentes legales y genera un primer borrador de un escrito de caso o una moción de juicio sumario.
*   **Script:**
    ```javascript
    // Generación de documentos conceptuales
    if (case.status === "Preparándose para el juicio") {
        const caseBriefDraft = DocumentGenerator.create('CaseBrief', {
            caseObject: case
        });
        // guardar caseBriefDraft en los archivos del caso
        createNote("Se ha generado el primer borrador del escrito del caso en función del estado actual del caso.");
    }
    ```

---

## **Parte 3: Automatización de hojas de cálculo con tecnología de inteligencia artificial**

Para este siguiente conjunto de scripts, asumimos una API más avanzada que permite la interacción directa con un modelo de IA y la hoja de cálculo del caso (`lexorcist_data.xlsx`).

**Nueva API asumida:**
*   `AI.analyze(model, params)`: Una función para llamar a un modelo de IA específico (p. ej., "Summarizer", "Sentiment", "LegalTopicClassifier").
*   `Spreadsheet.query(sheetName, query)`: Una función para ejecutar una consulta similar a SQL en una hoja.
*   `Spreadsheet.updateCell(sheetName, cell, value)`: Una función para actualizar una celda específica.
*   `Spreadsheet.appendRow(sheetName, rowData)`: Una función para agregar una nueva fila.
*   `Spreadsheet.createSheet(sheetName)`: Una función para crear una nueva hoja.

---

### **Nivel 11: IA para resumen y extracción de datos**

**41. Resumen de evidencia con tecnología de IA**
*   **Descripción:** Llama a un modelo de IA para generar un resumen conciso y neutral de una prueba larga y lo escribe en una columna "Resumen" en la hoja "Pruebas".
*   **Script:**
    ```javascript
    const longText = evidence.text;
    if (longText.length > 2000) { // Solo resumir textos largos
        const summary = AI.analyze("Summarizer", { text: longText });
        const evidenceRow = Spreadsheet.query("Evidence", `SELECT * WHERE EvidenceID = '${evidence.id}'`)[0];
        Spreadsheet.updateCell("Evidence", `H${evidenceRow.rowNumber}`, summary.text); // Suponiendo que H es la columna Resumen
    }
    ```

**42. Extracción de entidades clave a la hoja de cálculo**
*   **Descripción:** Utiliza una IA para extraer todas las entidades con nombre (Personas, Lugares, Organizaciones, Fechas) y rellena una nueva hoja "Entidades" con la entidad, su tipo y un enlace a la evidencia de origen.
*   **Script:**
    ```javascript
    Spreadsheet.createSheet("Entities"); // Falla silenciosamente si existe
    const entities = AI.analyze("EntityExtractor", { text: evidence.text });
    entities.forEach(entity => {
        Spreadsheet.appendRow("Entities", {
            "Entity": entity.name,
            "Type": entity.type,
            "SourceEvidenceID": evidence.id
        });
    });
    ```

**43. Rastreador de elementos de acción**
*   **Descripción:** Un script de IA que identifica elementos de acción o compromisos (p. ej., "Te pagaré el viernes") y los registra en una nueva hoja "Elementos de acción".
*   **Script:**
    ```javascript
    Spreadsheet.createSheet("ActionItems");
    const actionItems = AI.analyze("ActionItemFinder", { text: evidence.text });
    actionItems.forEach(item => {
        Spreadsheet.appendRow("ActionItems", {
            "Commitment": item.commitment,
            "DueDate": item.dueDate, // La IA extraería esto
            "SourceEvidenceID": evidence.id
        });
    });
    ```
**44. Desidentificar y anonimizar a la columna**
*   **Descripción:** Utiliza una IA para buscar y reemplazar toda la PII, guardando esta versión anónima en una nueva columna "Texto anonimizado" en la hoja de cálculo para compartirla de forma segura.
*   **Script:**
    ```javascript
    const anonymized = AI.analyze("Anonymizer", { text: evidence.text });
    const evidenceRow = Spreadsheet.query("Evidence", `SELECT * WHERE EvidenceID = '${evidence.id}'`)[0];
    Spreadsheet.updateCell("Evidence", `I${evidenceRow.rowNumber}`, anonymized.text); // Suponiendo que I es la columna Texto anonimizado
    ```

---

### **Nivel 12: IA para clasificación y puntuación**

**45. Análisis avanzado de emociones e intenciones**
*   **Descripción:** Analiza el texto en busca de emociones matizadas y la intención percibida, escribiendo estas puntuaciones en las columnas correspondientes en la hoja "Pruebas".
*   **Script:**
    ```javascript
    const analysis = AI.analyze("EmotionAndIntent", { text: evidence.text });
    const row = Spreadsheet.query("Evidence", `SELECT * WHERE EvidenceID = '${evidence.id}'`)[0];
    Spreadsheet.updateCell("Evidence", `J${row.rowNumber}`, analysis.emotion); // p. ej., "Ira"
    Spreadsheet.updateCell("Evidence", `K${row.rowNumber}`, analysis.intent);  // p. ej., "Engañoso"
    ```

**46. Clasificación de temas legales**
*   **Descripción:** Clasifica la evidencia en temas legales específicos, lo que permite un filtrado potente en la hoja de cálculo.
*   **Script:**
    ```javascript
    const topic = AI.analyze("LegalTopicClassifier", { text: evidence.text });
    const row = Spreadsheet.query("Evidence", `SELECT * WHERE EvidenceID = '${evidence.id}'`)[0];
    Spreadsheet.updateCell("Evidence", `L${row.rowNumber}`, topic.name); // p. ej., "Derecho contractual"
    ```

**47. Puntuación de admisibilidad de la evidencia**
*   **Descripción:** Evalúa la evidencia en función de las reglas de evidencia (rumores, relevancia) y agrega una "Puntuación de admisibilidad" preliminar y un razonamiento a la hoja de cálculo.
*   **Script:**
    ```javascript
    const admissibility = AI.analyze("AdmissibilityScorer", { text: evidence.text, metadata: evidence.metadata });
    const row = Spreadsheet.query("Evidence", `SELECT * WHERE EvidenceID = '${evidence.id}'`)[0];
    Spreadsheet.updateCell("Evidence", `M${row.rowNumber}`, admissibility.score); // p. ej., 0.75
    Spreadsheet.updateCell("Evidence", `N${row.rowNumber}`, admissibility.reasoning);
    ```

**48. Evaluador de la fuerza del argumento**
*   **Descripción:** Cuando la evidencia está vinculada a una alegación, esta IA evalúa qué tan fuertemente la evidencia la respalda, proporcionando una puntuación y una explicación en una columna "Fuerza de apoyo".
*   **Script:**
    ```javascript
    if (evidence.allegations.length > 0) {
        const allegationText = case.allegations.find(a => a.name === evidence.allegations[0]).description;
        const strength = AI.analyze("ArgumentStrength", { premise: evidence.text, conclusion: allegationText });
        const row = Spreadsheet.query("Evidence", `SELECT * WHERE EvidenceID = '${evidence.id}'`)[0];
        Spreadsheet.updateCell("Evidence", `O${row.rowNumber}`, strength.score);
    }
    ```

---

### **Nivel 13: IA para el análisis de casos basado en hojas de cálculo**

**49. Crear una hoja de cronología de casos dinámica**
*   **Descripción:** Consulta todas las pruebas, las ordena por fecha y utiliza un resumidor de IA en cada pieza para generar una nueva hoja de "Cronología" limpia.
*   **Script:**
    ```javascript
    Spreadsheet.createSheet("Timeline");
    const allEvidence = Spreadsheet.query("Evidence", "SELECT * ORDER BY Date ASC");
    allEvidence.forEach(row => {
        const summary = AI.analyze("Summarizer", { text: row.Text });
        Spreadsheet.appendRow("Timeline", {
            "Date": row.Date,
            "EventSummary": summary.text,
            "SourceEvidenceID": row.EvidenceID
        });
    });
    ```

**50. Identificar testigos clave a partir de los datos de la hoja de cálculo**
*   **Descripción:** Consulta la hoja "Entidades" para todas las entidades "Persona", cuenta sus menciones y crea una hoja "Testigos clave" ordenada por frecuencia.
*   **Script:**
    ```javascript
    Spreadsheet.createSheet("KeyWitnesses");
    const witnesses = Spreadsheet.query("Entities", "SELECT Entity, COUNT(*) as Mentions WHERE Type = 'Person' GROUP BY Entity ORDER BY Mentions DESC");
    witnesses.forEach(witness => {
        Spreadsheet.appendRow("KeyWitnesses", witness);
    });
    ```

**51. Matriz de contradicciones generada por IA**
*   **Descripción:** Compara sistemáticamente las pruebas en la hoja de cálculo y, si encuentra una contradicción, crea una hoja de "Matriz de contradicciones" que registra el conflicto.
*   **Script:**
    ```javascript
    Spreadsheet.createSheet("ContradictionMatrix");
    const allEvidence = Spreadsheet.query("Evidence", "SELECT EvidenceID, Text");
    for (let i = 0; i < allEvidence.length; i++) {
        for (let j = i + 1; j < allEvidence.length; j++) {
            const result = AI.analyze("ContradictionFinder", { textA: allEvidence[i].Text, textB: allEvidence[j].Text });
            if (result.isContradictory) {
                Spreadsheet.appendRow("ContradictionMatrix", {
                    "EvidenceID_A": allEvidence[i].EvidenceID,
                    "EvidenceID_B": allEvidence[j].EvidenceID,
                    "Explanation": result.explanation
                });
            }
        }
    }
    ```

**52. Agrupar pruebas similares en una nueva hoja**
*   **Descripción:** Utiliza un modelo de incrustación de IA para agrupar pruebas semánticamente similares, creando una hoja de "Clústeres" que asigna cada prueba a un ID de clúster.
*   **Script:**
    ```javascript
    Spreadsheet.createSheet("Clusters");
    const allEvidence = Spreadsheet.query("Evidence", "SELECT EvidenceID, Text");
    const clusters = AI.analyze("Clusterer", { documents: allEvidence.map(e => e.Text) });
    clusters.forEach((clusterId, index) => {
        Spreadsheet.appendRow("Clusters", {
            "EvidenceID": allEvidence[index].EvidenceID,
            "ClusterID": clusterId
        });
    });
    ```

---

### **Nivel 14: IA para tareas generativas y predictivas en la hoja de cálculo**

**53. Generar preguntas de deposición para un testigo**
*   **Descripción:** Selecciona un testigo de la hoja "Testigos clave", consulta todas las pruebas asociadas y utiliza una IA generativa para crear preguntas de deposición personalizadas en una nueva hoja.
*   **Script:**
    ```javascript
    const topWitness = Spreadsheet.query("KeyWitnesses", "SELECT Entity LIMIT 1")[0].Entity;
    Spreadsheet.createSheet(`DepoPrep_${topWitness}`);
    const relatedEvidence = Spreadsheet.query("Entities", `SELECT SourceEvidenceID WHERE Entity = '${topWitness}'`);
    const texts = relatedEvidence.map(e => Spreadsheet.query("Evidence", `SELECT Text WHERE EvidenceID = '${e.SourceEvidenceID}'`)[0].Text);

    const questions = AI.analyze("DepoQuestionGenerator", { context: texts.join("\n\n") });
    questions.forEach(q => {
        Spreadsheet.appendRow(`DepoPrep_${topWitness}`, { "Question": q });
    });
    ```

**54. Predecir los tipos de documentos que faltan**
*   **Descripción:** La IA analiza los documentos y las comunicaciones existentes en la hoja de cálculo para predecir qué documentos estándar probablemente falten, registrándolos en una hoja de "Brechas de descubrimiento".
*   **Script:**
    ```javascript
    Spreadsheet.createSheet("DiscoveryGaps");
    const allText = Spreadsheet.query("Evidence", "SELECT Text").map(r => r.Text).join("\n");
    const missingDocs = AI.analyze("MissingDocumentPredictor", { context: allText, caseType: case.type });
    missingDocs.forEach(doc => {
        Spreadsheet.appendRow("DiscoveryGaps", { "SuggestedMissingDocument": doc });
    });
    ```

**55. Detección de anomalías financieras en las transacciones**
*   **Descripción:** Si la hoja de cálculo tiene una hoja de "Transacciones", este script de IA la analiza en busca de anomalías, marcándolas para su revisión en una nueva hoja de "Transacciones marcadas".
*   **Script:**
    ```javascript
    Spreadsheet.createSheet("FlaggedTransactions");
    const transactions = Spreadsheet.query("Transactions", "SELECT *");
    const flagged = AI.analyze("FinancialAnomalyDetector", { transactions: transactions });
    flagged.forEach(txn => {
        Spreadsheet.appendRow("FlaggedTransactions", { ...txn, "Reason": "AI Flagged" });
    });
    ```

**56. Redactar un resumen del caso a partir de los datos de la hoja de cálculo**
*   **Descripción:** Un script de IA que lee varias hojas (Pruebas, Cronología, Alegaciones) para generar un primer borrador narrativo de un resumen del caso, escribiéndolo en una hoja de "Resumen del caso".
*   **Script:**
    ```javascript
    Spreadsheet.createSheet("CaseSummary");
    const context = {
        allegations: Spreadsheet.query("Allegations", "SELECT *"),
        timeline: Spreadsheet.query("Timeline", "SELECT *"),
        keyEvidence: Spreadsheet.query("Evidence", "SELECT * WHERE Severity = 'Critical'")
    };
    const summary = AI.analyze("CaseSummaryGenerator", { context: context });
    Spreadsheet.appendRow("CaseSummary", { "GeneratedSummary": summary.text });
    ```

---

### **Nivel 15: IA para funciones estratégicas e interactivas de hojas de cálculo**

**57. Análisis de escenarios "qué pasaría si"**
*   **Descripción:** Un usuario agrega evidencia hipotética a una hoja "Qué pasaría si". Este script utiliza una IA para predecir su impacto en el caso, escribiendo el análisis de nuevo en la hoja.
*   **Script:**
    ```javascript
    const scenarios = Spreadsheet.query("WhatIf", "SELECT * WHERE Analysis IS NULL");
    scenarios.forEach(scenario => {
        const impact = AI.analyze("ImpactPredictor", { case: case, newEvidence: scenario.HypotheticalEvidence });
        Spreadsheet.updateCell("WhatIf", `B${scenario.rowNumber}`, impact.analysis); // Suponiendo que B es la columna de análisis
    });
    ```

**58. Actualización automática del estado de la alegación en la hoja de cálculo**
*   **Descripción:** Revisa periódicamente todas las pruebas vinculadas a una alegación y, si están suficientemente respaldadas, actualiza una columna de "Estado" para esa alegación en la hoja "Alegaciones".
*   **Script:**
    ```javascript
    const allegations = Spreadsheet.query("Allegations", "SELECT *");
    allegations.forEach(allegation => {
        const relatedEvidence = Spreadsheet.query("Evidence", `SELECT * WHERE allegations LIKE '%${allegation.Name}%'`);
        const strength = AI.analyze("AllegationStrength", { evidence: relatedEvidence });
        if (strength.score > 0.8) {
            Spreadsheet.updateCell("Allegations", `C${allegation.rowNumber}`, "Sufficiently Supported"); // C es la columna de estado
        }
    });
    ```

**59. Identificar la evidencia "más persuasiva"**
*   **Descripción:** Una IA clasifica todas las pruebas en la hoja de cálculo en función de la relevancia, la admisibilidad y el impacto, y luego crea una hoja "Top5Evidence" con enlaces a estas piezas clave.
*   **Script:**
    ```javascript
    Spreadsheet.createSheet("Top5Evidence");
    const allEvidence = Spreadsheet.query("Evidence", "SELECT *");
    const rankings = AI.analyze("PersuasivenessRanker", { evidence: allEvidence, allegations: case.allegations });
    rankings.slice(0, 5).forEach(item => {
        Spreadsheet.appendRow("Top5Evidence", { "EvidenceID": item.id, "Rank": item.rank, "Reason": item.reason });
    });
    ```

**60. Consulta de hoja de cálculo con tecnología de IA a través del lenguaje natural**
*   **Descripción:** Un usuario escribe una pregunta en una hoja de "Consultas". El script la envía a una IA que la convierte en una consulta formal, la ejecuta y pega los resultados en la hoja.
*   **Script:**
    ```javascript
    const newQueries = Spreadsheet.query("Queries", "SELECT * WHERE Result IS NULL");
    newQueries.forEach(query => {
        const formalQuery = AI.analyze("NLQtoSQL", { question: query.Question });
        try {
            const results = Spreadsheet.query(formalQuery.sheet, formalQuery.sql);
            const resultsAsText = JSON.stringify(results, null, 2);
            Spreadsheet.updateCell("Queries", `B${query.rowNumber}`, resultsAsText);
        } catch (e) {
            Spreadsheet.updateCell("Queries", `B${query.rowNumber}`, `Error: ${e.message}`);
        }
    });
    ```
