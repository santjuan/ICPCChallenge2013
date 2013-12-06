public class NearGoldAdvisor implements Game.GoldAdvisor
{
	OriginalMap original;
	char[][] currentBoard = new char[16][25];
	char[][] currentBoardHoles = new char[16][25];
	int[] startDistanceO;
	int[] startDistanceH;
	boolean isRed;
	
	@Override
	public int getAdvice(Game game, long timeLeft, Game.DefenseAdvisor defenseAdvisor)
	{
		game.fillMap(currentBoard, true, true, true);
		game.fillMap(currentBoardHoles, false, false, true);
		try
		{
			return getGoldAvice(game);
		}
		finally
		{
			game.clearMap(currentBoard, true, true, true);
			game.clearMap(currentBoardHoles, false, false, true);
		}
	}

	@Override
	public void init(Game.InitialState initialState, Game initialGame, boolean red)
	{
		isRed = red;
		original = initialGame.getOriginalMap();
		for(int i = 0; i < 16; i++)
			for(int j = 0; j < 25; j++)
				currentBoard[i][j] = currentBoardHoles[i][j] = original.map[i][j]; 
		startDistanceO = original.getDiggingFast(initialGame.getPlayer(isRed).i, initialGame.getPlayer(isRed).j, 0, original.map);
		startDistanceH = original.getDiggingFast(initialGame.getPlayer(!isRed).i, initialGame.getPlayer(!isRed).j, 0, original.map);
	}
	
	@Override
	public int getQuickAdvice(Game game, long timeLeft, Game.DefenseAdvisor defenseAdvisor, boolean red)
	{
//		int nearestGold = Integer.MAX_VALUE;
//		int nearestGoldDir = Game.NONE;
//		int posI = game.getPlayer(isRed).i;
//		int posJ = game.getPlayer(isRed).j;
//		if(posI < 0)
//			return Game.NONE;
//		int[] myDistance = original.getDiggingFast(posI, posJ, game.getPlayer(isRed).digTime(game.getCurrentTurn()), game.getSimulator().map, game.getEnemies());		
//		boolean[] existingGold = new boolean[game.getGolds().length];
//		int indiceA = 0;
//		for(Game.GameGold g : game.getGolds())
//		{
//			int dist = OriginalMap.getDistance(g.i, g.j, myDistance);
//			if(g.existsFrom() <= game.getCurrentTurn() + dist)
//				existingGold[indiceA] = true;
//			indiceA++;
//		}
//		int indice = 0;
//		for(Game.GameGold g : game.getGolds())
//		{
//			int i = g.i; 
//			int j = g.j;
//			if(existingGold[indice])
//			{
//				int distance = OriginalMap.getDistance(i, j, myDistance);
//				int direction = OriginalMap.getDirection(i, j, myDistance);
//				if(OriginalMap.getDistance(original.getFalling(i, j, posI, posJ)) > 400) { indice++; continue; }
//			    if(nearestGold > distance)
//				{
//			    	if((((direction & OriginalMap.DIG_LEFT) != 0) || ((direction & OriginalMap.DIG_RIGHT) != 0)) && posI < 14 && currentBoard[posI + 1][posJ] == 'Z' && original.map[posI + 1][posJ] == '=') {indice++; continue;}
//					if(((direction & OriginalMap.DIG_LEFT) != 0) && posI < 14 && posJ >= 1 && currentBoard[posI + 1][posJ - 1] == 'Z' && original.map[posI + 1][posJ - 1] == '=') {indice++; continue;}
//					if(((direction & OriginalMap.DIG_RIGHT) != 0) && posI < 14 && posJ < 24 && currentBoard[posI + 1][posJ + 1] == 'Z' && original.map[posI + 1][posJ + 1] == '=') {indice++; continue;}
//					nearestGold = distance;
//					nearestGoldDir = direction;
//				}
//			}
//			indice++;
//		}
//		return nearestGoldDir;
		boolean isRedAnt = isRed;
		boolean cambio = false;
		if(red != isRed)
		{
			cambio = true;
			isRed = red;
			int[] tmp = startDistanceO;
			startDistanceO = startDistanceH;
			startDistanceH = tmp;
		}
		int adv = getAdvice(game, timeLeft, defenseAdvisor);
		if(cambio)
		{
			isRed = isRedAnt;
			int[] tmp = startDistanceO;
			startDistanceO = startDistanceH;
			startDistanceH = tmp;
		}
		return adv;
	}
	
	int getGoldAvice(Game game)
	{
		int nearestGold = Integer.MAX_VALUE;
		int nearestGoldDir = Game.NONE;
		int posI = game.getPlayer(isRed).i;
		int posJ = game.getPlayer(isRed).j;
		if(posI < 0)
			return Game.NONE;
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
			if(existingGold[indiceA++])
			{
				if(Game.IN_DEBUG)
					System.err.println("Found gold: " + i + " " + j);
				int distance = OriginalMap.getDistance(i, j, myDistance);
			    int indice = 0;
			    for(Game.GameGold ot : game.getGolds())
			    {
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
					nearestGoldDir = direction;
				}
			}
			indice++;
		}
		if(nearestGold > 400)
		{
			indice = 0;
			for(Game.GameGold g : game.getGolds())
			{
				int i = g.i; 
				int j = g.j;
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
						nearestGoldDir = direction;
					}
				}
				indice++;
			}
			indice = 0;
			for(Game.GameGold g : game.getGolds())
			{
				int i = g.i;
				int j = g.j;
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
						nearestGoldDir = direction;
					}
				}
			}
			indice = 0;
			for(Game.GameGold g : game.getGolds())
			{
				int i = g.i;
				int j = g.j;
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
						nearestGoldDir = Game.NONE;
					}
				}
			}
			indice = 0;
			for(Game.GameGold g : game.getGolds())
			{
				int i = g.i;
				int j = g.j;
				if(existingGold[indice++] && OriginalMap.getDistance(i, j, myDistance) > 400)
				{
					int distance = OriginalMap.getDistance(i, j, startDistance);
					distance += 60;
					if(nearestGold > distance)
					{
						if(Simulator.IN_DEBUG)
							System.err.println("Found gold last resource suicide: " + i + " " + j + " " + distance);
						nearestGold = distance;
						nearestGoldDir = Game.SUICIDE;
					}
				}
			}
		}
		if(Game.IN_DEBUG)
			System.err.println("Time taken: " + (System.currentTimeMillis() - time));
		if(nearestGold > 400)
		{
			if(Game.IN_DEBUG)
				System.err.println(posI + " " + posJ);
			if(Game.IN_DEBUG)	
				System.err.println("NO GOLD");
			return Game.NONE;
		}
		else
			return nearestGoldDir;
	}
}
