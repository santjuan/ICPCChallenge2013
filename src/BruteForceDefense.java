import java.util.ArrayList;

public class BruteForceDefense extends NearEnemiesDefenseBase
{
	final int searchDepth;
	int currentSearchDepth;
	UtilClasses.FastHash hashTmp;
	
	BruteForceDefense(int depth)
	{
		searchDepth = currentSearchDepth = depth;
	}
	
	@Override
	public void init(Game.InitialState initialState, Game initialGame, boolean red)
	{
		isRed = red;
		original = initialGame.getOriginalMap();
		for(int i = 0; i < 16; i++)
			for(int j = 0; j < 25; j++)
				originalMap[i][j] = currentBoard[i][j] = original.map[i][j];
		hashTmp = new UtilClasses.FastHash(originalMap);
	}
	
	@Override
	boolean doomPredicted(Game game, int advice, int tolerance) 
	{
		Simulator s = game.getSimulator().clonar(hashTmp);
		int posI = game.getPlayer(isRed).i;
		int posJ = game.getPlayer(isRed).j;
		int iE = game.getPlayer(!isRed).i;
		int jE = game.getPlayer(!isRed).j;
		if(posI >= 0 && iE >= 0)
		{
			boolean left = false;
			boolean right = false;
			if(Simulator.IN_DEBUG)
			for(int delta = 0; delta < 3; delta++)
			{
				if(iE >= 0 && posI == iE && posJ + delta == jE && posJ < 24 && canDig(iE, jE, 1, game, !isRed))
					left = true;
				if(iE >= 0 && posI == iE && posJ - delta == jE && posJ >= 1 && canDig(iE, jE, -1, game, !isRed))
					right = true;
			}
			if(left)
			{
				s.simulateTurn(new int[]{isRed ? advice : Game.DIG_LEFT, isRed ? Game.DIG_LEFT : advice});
				s.fallDown(isRed);
				if(s.doomed(currentSearchDepth - 1, new int[]{Game.LEFT,  Game.RIGHT, Game.DIG_LEFT, Game.DIG_RIGHT, Game.BOTTOM, Game.TOP, Game.NONE}, hashTmp, isRed) > tolerance)
					return true;
				if(right)
				{
					s = game.getSimulator().clonar(hashTmp);
					s.fallDown(isRed);
					s.simulateTurn(new int[]{isRed ? advice : Game.DIG_RIGHT, isRed ? Game.DIG_RIGHT : advice});
					if(s.doomed(currentSearchDepth - 1, new int[]{Game.LEFT,  Game.RIGHT, Game.DIG_LEFT, Game.DIG_RIGHT, Game.BOTTOM, Game.TOP, Game.NONE}, hashTmp, isRed) > tolerance)
						return true;
				}
				return false;
			}
			else if(right)
			{
				s.simulateTurn(new int[]{isRed ? advice : Game.DIG_RIGHT, isRed ? Game.DIG_RIGHT : advice});
				s.fallDown(isRed);
				if(s.doomed(currentSearchDepth - 1, new int[]{Game.LEFT,  Game.RIGHT, Game.DIG_LEFT, Game.DIG_RIGHT, Game.BOTTOM, Game.TOP, Game.NONE}, hashTmp, isRed) > tolerance)
					return true;
				else
					return false;
			}
		}
		s.simulateTurn(new int[]{isRed ? advice : Game.NONE, isRed ? Game.NONE : advice});
		s.fallDown(isRed);
		return s.doomed(currentSearchDepth, new int[]{Game.LEFT,  Game.RIGHT, Game.DIG_LEFT, Game.DIG_RIGHT, Game.BOTTOM, Game.TOP, Game.NONE}, hashTmp, isRed) > tolerance;
	}

	@Override
	int findSavingMove(Game game, int advice, int priority)
	{
		int[] order = new int[]{Game.NONE, Game.LEFT, Game.RIGHT, Game.DIG_LEFT, Game.DIG_RIGHT, Game.BOTTOM, Game.TOP};
		int indice = -1;
		for(int i = 0; i < order.length; i++)
			if(order[i] == advice)
				indice = i;
		if(indice != -1)
		{
			int tmp = order[priority];
			order[priority] = advice;
			order[indice] = tmp;
		}
		int posI = game.getPlayer(isRed).i;
		int posJ = game.getPlayer(isRed).j;
		int iE = game.getPlayer(!isRed).i;
		int jE = game.getPlayer(!isRed).j;
		boolean left = false;
		boolean right = false;
		if(posI >= 0 && iE >= 0)
		{
			if(Simulator.IN_DEBUG)
			for(int delta = 0; delta < 3; delta++)
			{
				if(iE >= 0 && posI == iE && posJ + delta == jE && posJ < 24 && canDig(iE, jE, 1, game, !isRed))
					left = true;
				if(iE >= 0 && posI == iE && posJ - delta == jE && posJ >= 1 && canDig(iE, jE, -1, game, !isRed))
					right = true;
			}
		}
		ArrayList <Integer> moves = new ArrayList <Integer> ();
		if(left) moves.add(Game.DIG_LEFT);
		if(right) moves.add(Game.DIG_RIGHT);
		if((left || right) && (posI == iE) && (posJ == jE))
		{
			ArrayList <Integer> movesA = new ArrayList <Integer> ();
			for(int v : order) movesA.add(v);
			movesA.remove(Integer.valueOf(Game.DIG_LEFT));
			movesA.add(0, Game.DIG_LEFT);
			movesA.remove(Integer.valueOf(Game.DIG_RIGHT));
			movesA.add(0, Game.DIG_RIGHT);
			order = new int[movesA.size()];
			for(int i = 0; i < order.length; i++) order[i] = movesA.get(i);
		}
		moves.add(Game.NONE);
		int[] realMoves = new int[moves.size()];
		for(int i = 0; i < moves.size(); i++)
			realMoves[i] = moves.get(i);
		int move = game.getSimulator().findSavingMove(moves.size() == 1 ? currentSearchDepth : currentSearchDepth - 1, order, hashTmp, isRed, realMoves);
		System.err.println("doomed " + advice + " " + move + " " + game.getPlayer(isRed).i + " " + game.getPlayer(isRed).j);
		if(move >= 0)
			return move;
		else
		{
			System.err.println("Totally doomed");
			return advice;
		}
	}

	@Override
	public void setMissedMoves(int missedMoves) 
	{
		if(missedMoves == 0)
			currentSearchDepth = searchDepth;
		else if(missedMoves == 1)
			currentSearchDepth = searchDepth - 1;
		else if(missedMoves <= 3)
			currentSearchDepth = searchDepth - 2;
		else
			currentSearchDepth = searchDepth - 3;
	}
}