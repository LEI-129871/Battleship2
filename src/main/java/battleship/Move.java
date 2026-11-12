package battleship;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.*;

/**
 * Shot
 *
 * @author Your Name
 * Date: 20/02/2026
 * Time: 19:39
 */
public class Move implements IMove {

	//-------------------------------------------------------------------
	private final int number;
	private final List<IPosition> shots;
	private final List<IGame.ShotResult> shotResults;

	//-------------------------------------------------------------------
	public Move(int moveNumber, List<IPosition> moveShots, List<IGame.ShotResult> moveResults) {
		this.number = moveNumber;
		this.shots = moveShots;
		this.shotResults = moveResults;
	}

	@Override
	public String toString() {
		return "Move{" +
				"number=" + number +
				", shots=" + shots.size() +
				", results=" + shotResults.size() +
				'}';
	}

	@Override
	public int getNumber() {
		return this.number;
	}

	@Override
	public List<IPosition> getShots() {
		return this.shots;
	}

	@Override
	public List<IGame.ShotResult> getShotResults() {
		return this.shotResults;
	}

	private void registerHit(IGame.ShotResult result, Map<String, Integer> hitsPerBoat, Map<String, Integer> sunkBoatsCount) {
		String boatName = result.ship().getCategory();
		hitsPerBoat.put(boatName,
				hitsPerBoat.getOrDefault(boatName, 0) + 1);

		if (result.sunk()) {
			// Navio afundado
			sunkBoatsCount.put(boatName,
					sunkBoatsCount.getOrDefault(boatName, 0) + 1);

			SoundPlayer.playSound("NavioAfundado.mp3");

		} else {
			// Navio atingido, mas não afundado
			SoundPlayer.playSound("NavioAtingido.mp3");
		}
	}

	/**
	 * Returns "s" when n is greater than 1, otherwise an empty string.
	 */
	private String plural(int n) {
		return n > 1 ? "s" : "";
	}

	/**
	 * Builds the human-readable summary of the enemy fire shown in verbose mode.
	 */
	private StringBuilder buildVerboseMessage(int validShots, int repeatedShots, int missedShots, int outsideShots,
											  Map<String, Integer> sunkBoatsCount, Map<String, Integer> hitsPerBoat) {
		// Construção da mensagem de saída
		StringBuilder output = new StringBuilder();

		if (validShots == 0 && repeatedShots > 0) {
			output.append(repeatedShots).append(" tiro").append(plural(repeatedShots)).append(" repetido").append(plural(repeatedShots));
		} else {
			if (validShots > 0) {
				output.append(validShots).append(" tiro").append(plural(validShots)).append(" válido").append(plural(validShots)).append(": ");
			}

			// Atualizar lógica para contar múltiplos barcos afundados do mesmo tipo
			for (Map.Entry<String, Integer> entry : sunkBoatsCount.entrySet()) {
				String boatName = entry.getKey();
				int count = entry.getValue();
				output.append(count).append(" ").append(boatName).append(plural(count)).append(" ao fundo").append(" + ");
			}

			for (Map.Entry<String, Integer> entry : hitsPerBoat.entrySet()) {
				String boatName = entry.getKey();
				int hits = entry.getValue();
				if (!sunkBoatsCount.containsKey(boatName)) {
					output.append(hits).append(" tiro").append(plural(hits)).append(" num(a) ").append(boatName).append(" + ");
				}
			}

			if (missedShots > 0) {
				output.append(missedShots).append(" tiro").append(plural(missedShots)).append(" na água");
			} else if (!sunkBoatsCount.isEmpty() || !hitsPerBoat.isEmpty()) {
				output.setLength(output.length() - 2); // Remover o "+" final
			}

			if (repeatedShots > 0) {
				output.append(", ");
				output.append(repeatedShots).append(" tiro").append(plural(repeatedShots)).append(" repetido").append(plural(repeatedShots));
			}
		}

		// Adicionar contagem de tiros fora do tabuleiro
		if (outsideShots > 0) {
			if (!output.isEmpty()) {
				output.append(", ");
			}
			output.append(outsideShots).append(" tiro").append(plural(outsideShots)).append(" exterior").append(outsideShots > 1 ? "es" : "");
		}

		return output;
	}

	/**
	 * Builds the list of sunk boats (type and count) for the JSON response.
	 */
	private List<Map<String, Object>> buildSunkBoats(Map<String, Integer> sunkBoatsCount) {
		List<Map<String, Object>> sunkBoats = new ArrayList<>();
		for (Map.Entry<String, Integer> entry : sunkBoatsCount.entrySet()) {
			Map<String, Object> boat = new HashMap<>();
			boat.put("type", entry.getKey());
			boat.put("count", entry.getValue());
			sunkBoats.add(boat);
		}
		return sunkBoats;
	}

	/**
	 * Builds the list of hits on boats that were not sunk for the JSON response.
	 */
	private List<Map<String, Object>> buildBoatHits(Map<String, Integer> hitsPerBoat, Map<String, Integer> sunkBoatsCount) {
		List<Map<String, Object>> boatHits = new ArrayList<>();
		for (Map.Entry<String, Integer> entry : hitsPerBoat.entrySet()) {
			if (!sunkBoatsCount.containsKey(entry.getKey())) {
				Map<String, Object> boat = new HashMap<>();
				boat.put("type", entry.getKey());
				boat.put("hits", entry.getValue());
				boatHits.add(boat);
			}
		}
		return boatHits;
	}

	/**
	 * Serializes the response map to an indented JSON string using Jackson.
	 */
	private String toJson(Map<String, Object> response) {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

		try {
			return objectMapper.writeValueAsString(response);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Erro ao serializar o JSON dos resultados da jogada", e);
		}
	}

	/**
	 * Processes the results of enemy fire on the game board, analyzing the outcomes of shots,
	 * such as valid shots, repeated shots, missed shots, hits on ships, and sunk ships. It can
	 * also display a detailed summary of the shot results if verbose mode is activated.
	 *
	 * @param verbose a boolean indicating whether a detailed summary should be printed to the console
	 *                for the processed enemy fire data.
	 * @return a JSON-formatted string that encapsulates the results, including counts of valid shots,
	 *         repeated shots, missed shots, shots outside the game board, and details of hits and
	 *         sunk ships.
	 */
	@Override
	public String processEnemyFire(boolean verbose) {

		int validShots = 0;
		int repeatedShots = 0;
		int missedShots = 0;

		Map<String, Integer> sunkBoatsCount = new HashMap<>(); // Rastrear quantos navios de cada tipo afundaram
		Map<String, Integer> hitsPerBoat = new HashMap<>();

		// Processar cada resultado de tiro
		for (IGame.ShotResult result : this.shotResults) {
			if (!result.valid()) {
				// Tiro inválido - apenas ignorar
				continue;
			}

			if (result.repeated()) {
				repeatedShots++; // Tiro repetido
			} else {
				// Tiro válido
				validShots++;

				if (result.ship() == null) {
					missedShots++; // Tiro na água
					SoundPlayer.playSound("Agua.mp3");

				} else {
					registerHit(result, hitsPerBoat, sunkBoatsCount);
				}
			}
		}

		// Determinar número de tiros fora do tabuleiro
		int outsideShots = Game.NUMBER_SHOTS - validShots - repeatedShots;

		if (verbose) {
			StringBuilder output = buildVerboseMessage(validShots, repeatedShots, missedShots, outsideShots,
					sunkBoatsCount, hitsPerBoat);

			// Imprimir na consola se verbose for true
			System.out.println("Jogada nº" + this.number + " -> " + output);
		}

		// Criar o mapa para o JSON
		Map<String, Object> response = new HashMap<>();
		response.put("validShots", validShots);
		response.put("outsideShots", outsideShots);
		response.put("repeatedShots", repeatedShots);
		response.put("missedShots", missedShots);
		response.put("sunkBoats", buildSunkBoats(sunkBoatsCount));
		response.put("hitsOnBoats", buildBoatHits(hitsPerBoat, sunkBoatsCount));

		String jsonString = toJson(response);

		System.out.println(jsonString);
		System.out.println();

		// Retornar o JSON
		return jsonString;
	}
}