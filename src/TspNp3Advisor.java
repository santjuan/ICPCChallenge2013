import java.util.ArrayList;
import java.util.BitSet;

public class TspNp3Advisor implements Game.GoldAdvisor
{	
	int[][] goldDistances;
	int[][] golds;
	int[] startDistanceO;
	int[] startDistanceH;
	char[][] currentBoard;
	char[][] currentMapHoles;
	boolean isRed;
	OriginalMap original;
	final int tspDepth;
	
	TspNp3Advisor(int depth) 
	{
		tspDepth = depth;
	}
	
	@Override
	public void init(Game.InitialState initialState, Game initialGame, boolean red) 
	{
		this.original = initialGame.getOriginalMap();
		isRed = red;
		char[][] original = initialGame.getOriginalMap().map;
		currentBoard = new char[16][25];
		currentMapHoles = new char[16][25];
		for(int i = 0; i < 16; i++)
			for(int j = 0; j < 25; j++)
				currentBoard[i][j] = currentMapHoles[i][j] = original[i][j];
		startDistanceO = this.original.getDiggingFast(initialGame.getPlayer(isRed).i, initialGame.getPlayer(isRed).j, 0, this.original.map);
		startDistanceH = this.original.getDiggingFast(initialGame.getPlayer(!isRed).i, initialGame.getPlayer(!isRed).j, 0, this.original.map);
		Game.GameGold[] goldsG = initialGame.getGolds();	
		golds = new int[goldsG.length][2];
		for(int i = 0; i < goldsG.length; i++)
		{
			golds[i][0] = goldsG[i].i;
			golds[i][1] = goldsG[i].j;
		}
		goldDistances = new int[golds.length][golds.length];
		int[] startDist = initialGame.getOriginalMap().getDiggingExact(initialGame.getPlayer(isRed).startI, initialGame.getPlayer(isRed).startJ, 0, currentBoard, currentMapHoles);
		for(int i = 0; i < golds.length; i++)
		{
			int[] dist = initialGame.getOriginalMap().getDiggingExact(golds[i][0], golds[i][1], 0, currentBoard, currentMapHoles);
			for(int j = 0; j < golds.length; j++)
			{
				int distanciaM = initialGame.getShortestRoute(dist, startDist, j, currentBoard, isRed);
				int distancia = OriginalMap.getDistance(distanciaM);
				goldDistances[i][j] = distancia;
			}
		}
	}
	
	int getGoldAvice(Game game, int posI, int posJ, ArrayList <Integer> forbidden)
	{
		int nearestGold = Integer.MAX_VALUE;
		int nearestGoldId = -1;
		if(posI < 0)
			return -1;
		long time = 0;
		if(Game.IN_DEBUG)
			System.currentTimeMillis();
		int[] myDistance = original.getDiggingFast(posI, posJ, game.getPlayer(isRed).digTime(game.getCurrentTurn()), currentBoard);
		if(Game.IN_DEBUG)
			System.err.println("Time taken sr0: " + (System.currentTimeMillis() - time));
		int[] startDistance = startDistanceO.clone();
		boolean[] invalidGold = new boolean[game.getGolds().length];
		boolean[] existingGold = new boolean[game.getGolds().length];
		int indiceA = 0;
		for(Game.GameGold g : game.getGolds())
		{
			if(forbidden.contains(indiceA)) {indiceA++; continue;}
			int dist = OriginalMap.getDistance(g.i, g.j, myDistance);
			if(dist > 400)
				dist = 60;
			if(g.existsFrom() <= game.getCurrentTurn() + dist)
				existingGold[indiceA] = true;
			indiceA++;
		}
		indiceA = 0;
		for(Game.GameGold g : game.getGolds())
		{
			int i = g.i;
			int j = g.j;
			if(forbidden.contains(indiceA)) {indiceA++; continue;}
			if(existingGold[indiceA++])
			{
				if(Game.IN_DEBUG)
					System.err.println("Found gold: " + i + " " + j);
				int distance = OriginalMap.getDistance(i, j, myDistance);
			    int indice = 0;
			    for(Game.GameGold ot : game.getGolds())
			    {
			    	if(forbidden.contains(indice)) {indice++; continue;}
			    	if(existingGold[indice])
			    	{
			    		int distanceO = OriginalMap.getDistance(ot.i, ot.j, myDistance);
					    int dist = original.getFalling(i, j, ot.i, ot.j) & OriginalMap.POSITION_MASK;
			    		int dist2 = original.getFalling(ot.i, ot.j, i, j) & OriginalMap.POSITION_MASK;
			    		if(dist < 5 && dist2 > 10 && distance - 5 < distanceO)
			    		{
			    			invalidGold[indice] = true;
			    			if(Game.IN_DEBUG)
			    				System.err.println("Invalidating " + ot.i + " " + ot.j + " " + i + " " + j);
			    		}
			    	}
			    	indice++;
			    }
			}
		}
		int indice = 0;
		for(Game.GameGold g : game.getGolds())
		{
			int i = g.i; 
			int j = g.j;
			if(forbidden.contains(indice)) {indice++; continue;}
			if(existingGold[indice] && !invalidGold[indice])
			{
				int distance = OriginalMap.getDistance(i, j, myDistance);
				int direction = OriginalMap.getDirection(i, j, myDistance);
				if(OriginalMap.getDistance(original.getFalling(i, j, posI, posJ)) > 400) { indice++; continue; }
			    if(nearestGold > distance)
				{
					if(((direction == Game.DIG_LEFT) || (direction == Game.DIG_RIGHT)) && posI < 14 && currentBoard[posI + 1][posJ] == 'Z' && original.map[posI + 1][posJ] == '=') {indice++; continue;}
					if((direction == Game.DIG_LEFT) && posI < 14 && posJ >= 1 && currentBoard[posI + 1][posJ - 1] == 'Z' && original.map[posI + 1][posJ - 1] == '=') {indice++; continue;}
					if((direction == Game.DIG_RIGHT) && posI < 14 && posJ < 24 && currentBoard[posI + 1][posJ + 1] == 'Z' && original.map[posI + 1][posJ + 1] == '=') {indice++; continue;}
					nearestGold = distance;
					nearestGoldId = indice;
				}
			}
			indice++;
		}
		if(nearestGold > 400 && forbidden.size() != 0) return -1;
		if(nearestGold > 400)
		{
			indice = 0;
			for(Game.GameGold g : game.getGolds())
			{
				int i = g.i; 
				int j = g.j;
				if(forbidden.contains(indice)) {indice++; continue;}
				if(existingGold[indice] && !invalidGold[indice])
				{
					int distance = OriginalMap.getDistance(i, j, myDistance);
					int direction = OriginalMap.getDirection(i, j, myDistance);
					if(nearestGold > distance)
					{
						if(((direction == Game.DIG_LEFT) || (direction == Game.DIG_RIGHT)) && posI < 14 && currentBoard[posI + 1][posJ] == 'Z' && original.map[posI + 1][posJ] == '=') {indice++; continue;}
						if((direction == Game.DIG_LEFT) && posI < 14 && posJ >= 1 && currentBoard[posI + 1][posJ - 1] == 'Z' && original.map[posI + 1][posJ - 1] == '=') {indice++; continue;}
						if((direction == Game.DIG_RIGHT) && posI < 14 && posJ < 24 && currentBoard[posI + 1][posJ + 1] == 'Z' && original.map[posI + 1][posJ + 1] == '=') {indice++; continue;}
						nearestGold = distance;
						nearestGoldId = indice;
					}
				}
				indice++;
			}
			indice = 0;
			for(Game.GameGold g : game.getGolds())
			{
				int i = g.i;
				int j = g.j;
				if(forbidden.contains(indice)) {indice++; continue;}
				if(!existingGold[indice++] && !game.getOriginalMap().isFalling(i, j))
				{
					int distance = OriginalMap.getDistance(i, j, myDistance);
					int direction = OriginalMap.getDirection(i, j, myDistance);
					if(OriginalMap.getDistance(original.getFalling(i, j, posI, posJ)) > 400) continue;
					distance += g.existsFrom() - (game.getCurrentTurn() + distance);
					if(Game.IN_DEBUG)
						System.err.println("Found gold last resource wait there: " + i + " " + j + " " + distance);
					if(nearestGold > distance)
					{
						if(((direction == Game.DIG_LEFT) || (direction == Game.DIG_RIGHT)) && posI < 14 && currentBoard[posI + 1][posJ] == 'Z' && original.map[posI + 1][posJ] == '=') {continue;}
						if((direction == Game.DIG_LEFT) && posI < 14 && posJ >= 1 && currentBoard[posI + 1][posJ - 1] == 'Z' && original.map[posI + 1][posJ - 1] == '=') {continue;}
						if((direction == Game.DIG_RIGHT) && posI < 14 && posJ < 24 && currentBoard[posI + 1][posJ + 1] == 'Z' && original.map[posI + 1][posJ + 1] == '=') {continue;}
						nearestGold = distance;
						nearestGoldId = indice - 1;
					}
				}
			}
			indice = 0;
			for(Game.GameGold g : game.getGolds())
			{
				int i = g.i;
				int j = g.j;
				if(forbidden.contains(indice)) {indice++; continue;}
				if(!existingGold[indice++])
				{
					int distance = OriginalMap.getDistance(i, j, myDistance);
					if(OriginalMap.getDistance(original.getFalling(i, j, posI, posJ)) > 400) continue;
					distance += g.existsFrom() - (game.getCurrentTurn() + distance);
					if(Game.IN_DEBUG)
						System.err.println("Found gold last resource wait here: " + i + " " + j + " " + distance);
					if(nearestGold > distance)
					{
						nearestGold = distance;
						nearestGoldId = indice - 1;
					}
				}
			}
			indice = 0;
			for(Game.GameGold g : game.getGolds())
			{
				int i = g.i;
				int j = g.j;
				if(forbidden.contains(indice)) {indice++; continue;}
				if(existingGold[indice++] && OriginalMap.getDistance(i, j, myDistance) > 400)
				{
					int distance = OriginalMap.getDistance(i, j, startDistance);
					distance += 60;
					if(nearestGold > distance)
					{
						if(Simulator.IN_DEBUG)
							System.err.println("Found gold last resource suicide: " + i + " " + j + " " + distance);
						nearestGold = distance;
						return -1;
					}
				}
			}
		}
		if(Game.IN_DEBUG)
			System.err.println("Time taken: " + (System.currentTimeMillis() - time));
		return nearestGoldId;
	}
	
	@Override
	public int getAdvice(Game game, long timeLeft, Game.DefenseAdvisor defenseAdvisor) 
	{
		return (Integer) getMove(game, timeLeft, defenseAdvisor)[1];
	}
	
	Object[] getMove(Game game, long timeLeft, Game.DefenseAdvisor defenseAdvisor) 
	{
		game.fillMap(currentBoard, true, true, true);
		game.fillMap(currentMapHoles, false, false, true);
		try
		{
			return getMove(game);
		}
		finally
		{
			game.clearMap(currentBoard, true, true, true);
			game.clearMap(currentMapHoles, false, false, true);
		}
	}

	@Override
	public int getQuickAdvice(Game game, long timeLeft, Game.DefenseAdvisor defenseAdvisor, boolean isRed) 
	{
		return Simulator.NONE;
	}
	
	int[] idsActuales;
	int[][] dp = new int[10][1 << 10];
	
	int[] tsp(int[] currentDist, int[] initialDist, int posI, int posJ, int[] ids, Game game)
	{
		idsActuales = ids;
		int mascaraTodos = (1 << ids.length) - 1;
		int mejor = Integer.MAX_VALUE;
		int mejorId = 0;
		for(int i = 0; i < ids.length; i++)
			for(int j = 0; j <= mascaraTodos; j++)
				dp[i][j] = -1;
		for(int i = 0; i < ids.length; i++)
		{
			int actual = (game.getShortestRoute(currentDist, initialDist, idsActuales[i], currentBoard, isRed) & OriginalMap.POSITION_MASK);
			actual += dp(i, mascaraTodos ^ (1 << i));
			if(actual < mejor)
			{
				mejor = actual;
				mejorId = idsActuales[i];
			}
		}
		return new int[]{mejorId, Math.min(401, mejor)};
	}
	
	private int dp(int ciudad, int mascara) 
	{
		if(dp[ciudad][mascara] >= 0) return dp[ciudad][mascara];
		if(mascara == 0) return dp[ciudad][mascara] = 0;
		int mejor = Integer.MAX_VALUE;
		for(int i = 0; i < idsActuales.length; i++)
		{
			if(((mascara & (1 << i)) != 0) && (goldDistances[idsActuales[ciudad]][idsActuales[i]] < 400))
			{
				int costo = dp(i, mascara ^ (1 << i)) + goldDistances[idsActuales[ciudad]][idsActuales[i]];
				if(costo < mejor) mejor = costo;
			}
		}
		return dp[ciudad][mascara] = mejor;
	}
	
	Object[] getMove(Game game)
	{
		int posI = game.getPlayer(isRed).i;
		int posJ = game.getPlayer(isRed).j;
		if(posI < 0) return new Object[]{-1, Game.NONE, 401, 401, null, 401};
		int[] initialDist = game.getOriginalMap().getDiggingExact(game.getPlayer(isRed).startI, game.getPlayer(isRed).startJ, 0, currentBoard, currentMapHoles);
		int[] currentDist = game.getOriginalMap().getDiggingExact(game.getPlayer(isRed).i, game.getPlayer(isRed).j, game.getPlayer(isRed).digTime(game.getCurrentTurn()), currentBoard, currentMapHoles);
		int[] currentDistHis = game.getPlayer(!isRed).i < 0 ? null : game.getOriginalMap().getDiggingExact(game.getPlayer(!isRed).i, game.getPlayer(!isRed).j, game.getPlayer(!isRed).digTime(game.getCurrentTurn()), currentBoard, currentMapHoles);
		ArrayList <Integer> oros = new ArrayList <Integer> ();
		int primero = getGoldAvice(game, posI, posJ, oros);
		if(primero == -1)
			return new Object[]{-1, Game.SUICIDE, 401, 401, null, 401};
		oros.add(primero);
		int dist = OriginalMap.getDistance(game.getShortestRoute(currentDist, initialDist, primero, currentBoard, isRed));
		int distHis = currentDistHis == null ? 401 : OriginalMap.getDistance(game.getShortestRoute(currentDistHis, startDistanceH, primero, currentBoard, isRed));
		int limite = (int) Math.max(Math.min(10, Math.ceil(dist / 2.5)), 1);
		limite = Math.max(4, limite);
		while(oros.size() < limite)
		{
			int ultimo = oros.get(oros.size() - 1);
			int iAct = golds[ultimo][0];
			int jAct = golds[ultimo][1];
			int mejorId = getGoldAvice(game, iAct, jAct, oros);
			if(mejorId == -1)
				break;
			else
				oros.add(mejorId);
		}
		int[] idsEscogidos = new int[oros.size()];
		for(int i = 0; i < idsEscogidos.length; i++)
			idsEscogidos[i] = oros.get(i);
		BitSet importants = new BitSet();
		for(int i : idsEscogidos)
			importants.set(i);
		int[] tspA = tsp(currentDist, initialDist, posI, posJ, idsEscogidos, game);
		int idSiguiente = tspA[0];
		int selected = OriginalMap.getDirection(game.getShortestRoute(currentDist, initialDist, idSiguiente, currentBoard, isRed));
		return new Object[]{idSiguiente, selected, dist, distHis, importants, tspA[1]};
	}
}