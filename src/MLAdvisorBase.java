import java.util.BitSet;

public abstract class MLAdvisorBase implements Game.GoldAdvisor
{
	int[][] goldDistances;
	int[][] golds;
	BitSet[] goldImportants;
	int[] invalidates;
	int[] nearGolds;
	int[] startDistanceO;
	int[] startDistanceH;
	char[][] currentBoard;
	char[][] currentMapHoles;
	boolean isRed;
	OriginalMap original;
	TspNp3Advisor tspAdvisor;
	ZombieLadders zombieLadders;
	
	boolean[] getExisting(Game game)
	{
		boolean[] existing = new boolean[game.getGolds().length];
		int indiceA = 0;
		int[] myDistance = game.getOriginalMap().getDiggingFast(game.getPlayer(isRed).i, game.getPlayer(isRed).j, game.getPlayer(isRed).digTime(game.getCurrentTurn()), currentBoard);
		for(Game.GameGold g : game.getGolds())
		{
			int dist = OriginalMap.getDistance(g.i, g.j, myDistance);
			if(dist > 400)
				dist = 60;
			if(g.existsFrom() <= game.getCurrentTurn() + dist)
				existing[indiceA] = true;
			indiceA++;
		}
		return existing;
	}
	
	@Override
	public void init(Game.InitialState initialState, Game initialGame, boolean red) 
	{
		tspAdvisor = new TspNp3Advisor(4);
		tspAdvisor.init(initialState, initialGame, red);
		this.original = initialGame.getOriginalMap();
		isRed = red;
		char[][] original = initialGame.getOriginalMap().map;
		currentBoard = new char[16][25];
		currentMapHoles = new char[16][25];
		for(int i = 0; i < 16; i++)
			for(int j = 0; j < 25; j++)
				currentBoard[i][j] = currentMapHoles[i][j] = original[i][j];
		startDistanceO = this.original.getDiggingFast(initialGame.getPlayer(isRed).startI, initialGame.getPlayer(isRed).startJ, 0, this.original.map);
		startDistanceH = this.original.getDiggingFast(initialGame.getPlayer(!isRed).startI, initialGame.getPlayer(!isRed).startJ, 0, this.original.map);
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
		invalidates = new int[golds.length];
		nearGolds = new int[golds.length];
		goldImportants = new BitSet[golds.length];
		for(int i = 0; i < golds.length; i++) 
			goldImportants[i] = new BitSet();
		int indiceA = 0;
		for(Game.GameGold g : initialGame.getGolds())
		{
			int i = g.i;
			int j = g.j;
			int invalidated = 0;
			int near = 0;
			int indiceB = 0;
		    for(Game.GameGold ot : initialGame.getGolds())
		    {
	    		int dist = OriginalMap.getDistance(this.original.getFalling(i, j, ot.i, ot.j));
	    		int dist2 = OriginalMap.getDistance(this.original.getFalling(ot.i, ot.j, i, j));
	    		if(dist < 5 && dist2 > 10 && !(ot.i == g.i && ot.j == g.j))
	    		{
	    			invalidated++;
	    			goldImportants[indiceA].set(indiceB);
	    		}
	    		if(dist <= 5 && dist2 <= 5 && !(ot.i == g.i && ot.j == g.j))
	    		{
	    			near++;
	    			goldImportants[indiceA].set(indiceB);
		    	}
	    		indiceB++;
		    }
		    invalidates[indiceA] = invalidated;
		    nearGolds[indiceA] = near;
		    indiceA++;
		}
		zombieLadders = new ZombieLadders(initialGame.getOriginalMap(), initialGame.getEnemies());
	}
	
	class MLEntry
	{
		double myDistance;
		double enemyDistance;
		double nearGolds;
		double nearest;
		double choosen;
		double zombiesInLadders;
		double reward;
		double tspSize;
		BitSet importants = new BitSet();
		
		MLEntry(Game game, int id, int[] myDistanceV, int[] hisDistance, Object[] tspAns)
		{
			nearest = (Integer) tspAns[2];
			choosen = (((Integer) tspAns[0]).intValue()) == id ? 1 : 0;
			nearGolds = MLAdvisorBase.this.nearGolds[id] + 2 * invalidates[id];
			myDistance = OriginalMap.getDistance(game.getShortestRoute(myDistanceV, startDistanceO, id, currentBoard, isRed));
			tspSize = choosen == 0 ? 0 : (Integer) tspAns[5];
			if(game.getPlayer(!isRed).i < 0)
				enemyDistance = game.getPlayer(!isRed).existsFrom(game.getCurrentTurn()) - game.getCurrentTurn() + OriginalMap.getDistance(game.getShortestRoute(startDistanceH, startDistanceH, id, currentBoard, !isRed));
			else
				enemyDistance = OriginalMap.getDistance(game.getShortestRoute(hisDistance, startDistanceH, id, currentBoard, !isRed));
			myDistance = Math.min(400, myDistance);
			enemyDistance = Math.min(400, enemyDistance);
			reward = 0;
			zombiesInLadders = zombieLadders.getDiggingFast(game.getPlayer(isRed).i, game.getPlayer(isRed).j, game.getPlayer(isRed).digTime(game.getCurrentTurn()), currentBoard)[golds[id][0] * 25 + golds[id][1]];
			importants.or(goldImportants[id]);
			if(choosen == 1 && tspAns[4] != null)
				importants.or((BitSet) tspAns[4]);
			importants.set(id);
		}
		
		double[] getVals()
		{
			return new double[]{myDistance, enemyDistance, nearGolds, choosen, tspSize, zombiesInLadders};
		}
	}
}