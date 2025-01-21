package com.example.bigdataback.ollama;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OllamaService {

    private final ChatModel chatModel;

    private static final String PROMPT_TEMPLATE = """
            Tu es un assistant spécialisé qui corrige et reformule les requêtes en français pour qu’elles respectent strictement les formats suivants :
            
            - prix inférieur à <nombre>
            - prix supérieur à <nombre>
            - prix égal à <nombre>
            - note supérieure à <nombre>
            - note inférieure à <nombre>
            - note égal à <nombre>
            - catégorie principale est <valeur>
            - catégorie contient <valeur>
            - magasin est <valeur>
            - titre contient <valeur>
            - description contient <valeur>
            
            Les conditions peuvent être combinées uniquement avec "et" ou "ou", sans utiliser de virgules.
            
            Instructions :
            1. Corrige uniquement l’orthographe et la grammaire.
            2. Remplace les synonymes et les expressions variées pour correspondre exactement aux formats ci-dessus. Remplace également les virgules décimales par des points.
            3. Ne renvoie que la phrase corrigée adaptée au parser.
            4. Élimine toutes les unités de mesure(comme "euro", "kg", etc.) et toute autre information supplémentaire.
            5. Ne fournit aucune explication, commentaire ou information additionnelle.
            6. Ne modifie pas les noms propres, les marques, les magasins ou tout autre terme spécifique qui pourrait être une entité unique.
            7. Ne fusionne pas ou ne divise pas les mots de manière incorrecte (ex. "magazin Larcele" doit rester "magasin Larcele").
            8. Si une partie de la phrase est incomplète ou incohérente, enlève cette partie pour ne conserver que les segments complets et pertinents.
            9. Assure-toi que la phrase résultante correspond exactement à une ou plusieurs des expressions suivantes :
                - prix inférieur à <nombre>
                - prix supérieur à <nombre>
                - prix égal à <nombre>
                - note supérieure à <nombre>
                - note inférieure à <nombre>
                - note égal à <nombre>
                - catégorie principale est <valeur>
                - catégorie contient <valeur>
                - magasin est <valeur>
                - titre contient <valeur>
                - description contient <valeur>
                - Combinaisons de ces expressions uniquement avec "et" ou "ou", sans utiliser de virgules.
            10. Les mots-clés suivants doivent toujours être en minuscules :"prix", "note", "supérieur", "inférieur", "égal", "catégorie", "magasin", "titre", "description".
            
            Exemples :
            - Entrée :Prix superieur a 50 euro et magasin est Amazon
              Sortie :prix supérieur à 50 et magasin est Amazon
            
            - Entrée :Je cherche des produitsdans magazin Larcele or superieur a 4 ???
              Sortie :magasin est Larcele ou note supérieure à 4
            
            - Entrée :prix inférieur a 100, note supérieure à 4 et magasin est Fnac
              Sortie :prix inférieur à 100 et note supérieure à 4 et magasin est Fnac
            
            - Entrée :Note superieur a 5, prix egal a 20 ou magasin est Fnac
              Sortie :note supérieure à 5 ou prix égal à 20 ou magasin est Fnac
            
            Voici la phrase à corriger :
            %s
            """;

    public String refactorUserRequest(String request) {
        String prompt = String.format(PROMPT_TEMPLATE, request);
        log.debug("Prompt send to Ollama : {}", prompt);

        try {
            String response = chatModel.call(prompt).trim();
            log.debug("Response of Ollama : {}", response);

            if (response.isEmpty()) {
                log.warn("Ollama response is empty : {}", request);
                return "";
            }

            return response;
        } catch (Exception e) {
            log.error("An error is occurred when refactoring the request: {}", request, e);
            return "";
        }
    }
}