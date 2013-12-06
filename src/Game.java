public abstract class Game
{
	static boolean IN_DEBUG = false;
	static final int TOP = 0;
	static final int BOTTOM = 1;
	static final int LEFT = 2;
	static final int RIGHT = 3;
	static final int DIG_LEFT = 4;
	static final int DIG_RIGHT = 5;
	static final int NONE = 6;
	static final int SUICIDE = 7;

	static abstract class GameGold
	{
		int i, j;
		
		abstract int existsFrom();
		abstract boolean exists(int currentTurn);
	}
	
	static abstract class GameEnemy 
	{
		int i, j, master, startI, startJ;
		
		abstract int existsFrom();
		abstract boolean canMove(int currentTurn);
	}
	
	static abstract class GamePlayer 
	{
		int i, j, startI, startJ, score, lastGoldI, lastGoldJ;
		
		abstract int digTime(int currentTurn);
		abstract int existsFrom(int currentTurn);
	}
	
	static abstract class GameHole 
	{
		int i, j;
		
		abstract int existsUntil();
		abstract boolean madeBy(boolean red);
	}
	
	static interface GoldAdvisor
	{
		abstract int getAdvice(Game game, long timeLeft, DefenseAdvisor defenseAdvisor);
		abstract void init(Game.InitialState initialState, Game initialGame, boolean isRed);
		abstract int getQuickAdvice(Game game, long timeLeft, DefenseAdvisor defenseAdvisor, boolean isRed);
	}
	
	static interface DefenseAdvisor
	{
		abstract int getAdvice(Game game, int goldChoice, long timeLeft, GoldAdvisor goldAdvisor);
		abstract void init(Game.InitialState initialState, Game initialGame, boolean isRed);
		abstract int getQuickAdvice(Game game, int goldChoice, long timeLeft, GoldAdvisor goldAdvisor, boolean isRed);
		abstract void setMissedMoves(int missedMoves);
	}
	
	static abstract class Strategy
	{
		private DefenseAdvisor basicDefense = new NearEnemiesDefenseBase() 
		{	
			@Override
			public void setMissedMoves(int missedMoves) 
			{
				return;
			}
			
			@Override
			public void init(InitialState initialState, Game initialGame, boolean red) 
			{
				isRed = red;
				original = initialGame.getOriginalMap();
				for(int i = 0; i < 16; i++)
					for(int j = 0; j < 25; j++)
						originalMap[i][j] = currentBoard[i][j] = original.map[i][j];
			}
			
			@Override
			int findSavingMove(Game game, int advice, int priority) 
			{
				return advice;
			}
			
			@Override
			boolean doomPredicted(Game game, int advice, int tolerance) 
			{
				return false;
			}
		};
		private GoldAdvisor basicGold = new GoldAdvisor() 
		{	
			@Override
			public void init(InitialState initialState, Game initialGame, boolean isRed) 
			{
				return;
			}
			
			@Override
			public int getQuickAdvice(Game game, long timeLeft, DefenseAdvisor defenseAdvisor, boolean isRed) 
			{
				return getAdvice(game, timeLeft, defenseAdvisor);
			}
			
			@Override
			public int getAdvice(Game game, long timeLeft, DefenseAdvisor defenseAdvisor) 
			{
				GamePlayer p = game.getPlayer(true);
				if(p.i < 0) return Game.NONE;
				int nearestGold = Integer.MAX_VALUE;
				int nearestGoldDir = Game.NONE;
				char[][] currentBoard = new char[16][25];
				for(int i = 0; i < 16; i++)
					for(int j = 0; j < 25; j++)
						currentBoard[i][j] = game.getOriginalMap().map[i][j];
				game.fillMap(currentBoard, true, true, true);
				for(GameGold g : game.getGolds())
				{
					int d = game.getOriginalMap().getDiggingFast(p.i, p.j, p.digTime(game.getCurrentTurn()), currentBoard)[g.i * 25 + g.j];
					int dist = OriginalMap.getDistance(d);
					if(dist < nearestGold && g.exists(game.getCurrentTurn()))
					{
						nearestGold = dist;
						nearestGoldDir = OriginalMap.getDirection(d);
					}
				}
				return nearestGoldDir;
			}
		};
		DefenseAdvisor defense;
		GoldAdvisor gold;
		
		int getPlay(Game state)
		{
			try
			{
				defense.setMissedMoves(state.getMissedMoves());
			}
			catch(Exception e)
			{
				System.err.println("Unexpected exception defense missing " + e.getMessage());
			}
			int goldMove = Game.NONE;
			try
			{
				goldMove = gold.getAdvice(state, 120, defense);
			}
			catch(Exception e)
			{
				System.err.println("Unexpected exception gold " + e.getMessage());
				try
				{
					goldMove = basicGold.getAdvice(state, 120, defense);
				}
				catch(Exception e1)
				{
					System.err.println("Unexpected exception gold basic " + e.getMessage());
					goldMove = Game.NONE;
				}
			}
			try
			{
				return defense.getAdvice(state, goldMove, 120, gold);
			}
			catch(Exception e)
			{
				System.err.println("Unexpected exception defense " + e.getMessage());
				try
				{
					basicDefense.init(null, state, true);
					return basicDefense.getAdvice(state, goldMove, 120, gold);
				}
				catch(Exception e1)
				{
					System.err.println("Unexpected exception basic defense " + e.getMessage());
					return goldMove;
				}
			}
		}
		
		abstract void init(Game state, Game.InitialState initialState, boolean isRed);
	}
	
	static class Position
	{
		int i, j;
		
		Position(int ii, int jj)
		{
			i = ii;
			j = jj;
		}
	}

	static class InitialState
	{
		int turnCount;
		char[][] startingBoard;
		char[][] enemiesProgram;
		Position[] enemiesStarting;
		Position redStarting;
		Position blueStarting;
		
		public InitialState(UtilClasses.Scanner sc)
		{
			turnCount = sc.nextInt();
			startingBoard = new char[16][];
			for(int i = 0; i < 16; i++)
				startingBoard[i] = sc.next().toCharArray();
			redStarting = new Position(sc.nextInt(), sc.nextInt());
			blueStarting = new Position(sc.nextInt(), sc.nextInt());
			int n = sc.nextInt();
			enemiesProgram = new char[n][];
			enemiesStarting = new Position[n];
			for(int i = 0; i < n; i++)
			{
				enemiesStarting[i] = new Position(sc.nextInt(), sc.nextInt());
				enemiesProgram[i] = sc.next().toCharArray();
			}
		}
		
		public InitialState()
		{
		}
	}
	
	int getShortestRoute(int[] currentDistance, int[] startDistance, int idGold, char[][] currentBoard, boolean isRed)
	{
		GameGold gold = getGolds()[idGold];
		int targetI = gold.i;
		int targetJ = gold.j;
		int extra = 0;
		int distanciaR = currentDistance[targetI * 25 + targetJ];
		int distancia = OriginalMap.getDistance(distanciaR);
		int direccion = distanciaR & OriginalMap.DIRECTION_MASK;
		int tiempoLlegada = getCurrentTurn() + distancia;
		if(!gold.exists(tiempoLlegada))
		{
			extra = getGolds()[idGold].existsFrom() - tiempoLlegada;
			if(getOriginalMap().isFalling(targetI, targetJ)) 
			{
				int posI = getPlayer(isRed).i;
				int posJ = getPlayer(isRed).j;
				if(posI >= 0)
				{
					int dir = OriginalMap.getDirection(direccion);
					if(dir == Game.LEFT)
						posJ--;
					else if(dir == Game.RIGHT)
						posJ++;
					else if(dir == Game.BOTTOM)
						posI++;
					else if(dir == Game.TOP)
						posI--;
					while(posI >= 0 && posI < 16 && posJ >= 0 && posJ < 25 && getOriginalMap().isFalling(posI, posJ, currentBoard))
					{
						if(posI == targetI && posJ == targetJ)
							direccion = OriginalMap.NONE << 16;
						posI++;
					}
					if(posI == targetI && posJ == targetJ)
						direccion = OriginalMap.NONE << 16;
				}
			}
		}
		if(distancia + extra > 400)
		{
			int distanciaInicioR = startDistance[targetI * 25 + targetJ];
			int distanciaInicio = OriginalMap.getDistance(distanciaInicioR);
			distanciaInicio += 60;
			if(distanciaInicio < distancia)
			{
				distancia = distanciaInicio;
				direccion = OriginalMap.SUICIDE << 16;
				tiempoLlegada = getCurrentTurn() + distancia;
				extra = 0;
			}
		}
		distancia += extra;
		if(distancia > 400)
		{
			distanciaR = getOriginalMap().getDiggingFast(getPlayer(isRed).i, getPlayer(isRed).j, getPlayer(isRed).digTime(getCurrentTurn()), currentBoard)[targetI * 25 + targetJ];
			distancia = OriginalMap.getDistance(distanciaR) + Math.max(extra, 25);
			direccion = distanciaR & OriginalMap.DIRECTION_MASK;
		}
		if(distancia > 400)
			distancia = 401;
		distancia |= direccion;
		return distancia;
	}

	void fillMap(char[][] map, boolean golds, boolean enemies, boolean holes)
	{
		int currentTurn = getCurrentTurn();
		char[][] original = getOriginalMap().map;
		if(golds)
			for(GameGold g : getGolds()) if(g.exists(currentTurn)) map[g.i][g.j] = '*';
		if(holes)
			for(GameHole h : getHoles()) if(h.i >= 0) map[h.i][h.j] = '-';
		if(enemies)
		{
			GameEnemy[] en = getEnemies();
			for(GameEnemy e : en) if(e.i >= 0) map[e.i][e.j] = 'Z';
			for(GameEnemy e : en) 
				if(e.i >= 0 && e.i < 15 && map[e.i + 1][e.j] == '-' && original[e.i][e.j] == '.')
				{
					map[e.i][e.j] = '.';
					map[e.i + 1][e.j] = 'Z'; 
				}
			if(golds)
			{
				for(GameGold g : getGolds()) 
					if(g.exists(currentTurn) && map[g.i][g.j] == '.') map[g.i][g.j] = '*';
			}
		}
	}
	
	void clearMap(char[][] map, boolean golds, boolean enemies, boolean holes)
	{
		int currentTurn = getCurrentTurn();
		char[][] original = getOriginalMap().map;
		if(golds)
			for(GameGold g : getGolds()) if(g.exists(currentTurn)) map[g.i][g.j] = original[g.i][g.j];
		if(holes)
			for(GameHole h : getHoles()) if(h.i >= 0) map[h.i][h.j] = original[h.i][h.j];
		if(enemies)
		{
			GameEnemy[] en = getEnemies();
			for(GameEnemy e : en) if(e.i >= 0) map[e.i][e.j] = original[e.i][e.j];
			for(GameEnemy e : en) if(e.i >= 0 && e.i < 15) map[e.i + 1][e.j] = original[e.i + 1][e.j]; 
		}
	}
	
	abstract int getCurrentTurn();
	abstract boolean isReal();
	abstract GameGold[] getGolds();
	abstract GameEnemy[] getEnemies();
	abstract GamePlayer getPlayer(boolean red);
	abstract GameHole[] getHoles();
	abstract OriginalMap getOriginalMap();
	abstract Simulator getSimulator();
	abstract int getMissedMoves();
}