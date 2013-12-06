public class ZombieDefense 
{
	final char[][] currentBoard = new char[16][25];
	final char[][] originalMap = new char[16][25];
	OriginalMap original;
	boolean isRed;
	boolean canSimulate = true;
	
	final boolean trappedZombie(int i, int j) 
	{
		return originalMap[i][j] == '=';
	}
	
	final boolean isPoisoned(int posI, int posJ, Game state)
	{
		if(posI == 16) return true;
		if(posI == -1) return true;
		if(posJ == -1) return true;
		if(posJ == 25) return true;
		if(currentBoard[posI][posJ] == 'Z' && !(posI < 15 && currentBoard[posI + 1][posJ] == '-')) return true;
		if(posI - 1 >= 0 && currentBoard[posI - 1][posJ] == 'Z' && !trappedZombie(posI - 1, posJ) && zombieCanMove(posI - 1, posJ, 0, 0, state)) return true;
		if(posI + 1 < 16 && currentBoard[posI + 1][posJ] == 'Z' && !trappedZombie(posI + 1, posJ) && zombieCanMove(posI + 1, posJ, 0, 0, state)) return true;
		if(posJ - 1 >= 0 && currentBoard[posI][posJ - 1] == 'Z' && !trappedZombie(posI, posJ - 1) && zombieCanMove(posI, posJ - 1, 0, 0, state)) return true;
		if(posJ + 1 < 25 && currentBoard[posI][posJ + 1] == 'Z' && !trappedZombie(posI, posJ + 1) && zombieCanMove(posI, posJ + 1, 0, 0, state)) return true;
		if(posI + 1 == 15 && currentBoard[posI + 1][posJ] == '-') return true;
		if(posI + 1 < 14 && currentBoard[posI + 1][posJ] == '-' && currentBoard[posI + 2][posJ] == '=')
			for(Game.GameHole h : state.getHoles()) if(h.i == posI + 1 && h.j == posJ && h.madeBy(!isRed)) return true;
		return false;
	}
	
	final boolean standsSolid(int posI, int posJ) 
	{
		return posI == 15 || currentBoard[posI + 1][posJ] == '=' || currentBoard[posI + 1][posJ] == 'H';
	}
	
	final boolean canDig(int posI, int posJ, int delta, Game state, boolean isRed) 
	{
		return (standsSolid(posI, posJ) || originalMap[posI][posJ] == 'H') && state.getPlayer(isRed).digTime(state.getCurrentTurn()) == 0 && posJ + delta >= 0 && posJ + delta < 25 && posI < 15 && currentBoard[posI + 1][posJ + delta] == '=' && (currentBoard[posI][posJ + delta] == 'Z' || currentBoard[posI][posJ + delta] == '.' || currentBoard[posI][posJ + delta] == '*') && (originalMap[posI][posJ + delta] == '.' || originalMap[posI][posJ + delta] == '*');
	}
	
	final boolean zombieCanMove(int posI, int posJ, int deltaI, int deltaJ, Game state)
	{
		for(Game.GameEnemy e : state.getEnemies())
			if(e.i >= 0 && e.i == posI + deltaI && e.j == posJ + deltaJ)
				if(e.canMove(state.getCurrentTurn()))
					return true;
		return false;
	}
	
	final boolean zombieNear(int posI, int posJ, int deltaI, int deltaJ)
	{
		return posJ + deltaJ >= 0 && posJ + deltaJ < 25 && posI + deltaI >= 0 && posI + deltaI < 16 && currentBoard[posI + deltaI][posJ + deltaJ] == 'Z' && !trappedZombie(posI + deltaI, posJ + deltaJ) && !(posI + deltaI < 15 && currentBoard[posI + deltaI + 1][posJ + deltaJ] == '-');
	}
	
	final char getCurrentContent(int i, int j)
	{
		if(currentBoard[i][j] == 'Z') return 'Z';
		else if(currentBoard[i][j] != '.' && currentBoard[i][j] != 'H' && currentBoard[i][j] != '=') return '.';
		else return currentBoard[i][j];
	}
	
	final boolean isFalling(int i, int j)
	{
		return i < 15 && getCurrentContent(i, j) == '.' && getCurrentContent(i + 1, j) == '.';
	}
	
	final int fallsInto(int fromI, int fromJ)
	{
		while(fromI < 15 && getCurrentContent(fromI + 1, fromJ) == '.') fromI++;
		return fromI;
	}
	
	final boolean isBadFall(int posI, int posJ, Game state)
	{
		if(canSimulate)
			return false;
		else
		{
			int fallRow = fallsInto(posI + 1, posJ);
			for(Game.GameEnemy p : state.getEnemies())
				if(p.i >= 0)
				{
					int limit = 0;
					if((original.getReversible(fallRow, posJ, p.i, p.j) & OriginalMap.POSITION_MASK) <= limit) return true;
				}
			return false;
		}
	}

	final int tryMoveUp(int posI, int posJ, Game state)
	{
		boolean poisoned = isPoisoned(posI - 1, posJ, state);
		if(!poisoned)
			return Game.TOP;
		else
		{
			if(posI < 15 && currentBoard[posI + 1][posJ] != '=' && !isPoisoned(posI + 1, posJ, state))
				return Game.BOTTOM;
			else if(posJ < 24 && currentBoard[posI][posJ + 1] != '=' && !isPoisoned(posI, posJ + 1, state))
				return Game.RIGHT;
			else
				return Game.LEFT;
		}
	}

	final int tryMoveDown(int posI, int posJ, Game state)
	{
		boolean poisoned = isPoisoned(posI + 1, posJ, state);
		if(!poisoned && isFalling(posI + 1, posJ))
			poisoned = isBadFall(posI + 1, posJ, state);
		if(!poisoned)
			return Game.BOTTOM;
		else
		{
			if(posI - 1 >= 0 && (currentBoard[posI - 1][posJ] == 'H' || currentBoard[posI - 1][posJ] == '.') && !isPoisoned(posI - 1, posJ, state))
				return Game.TOP;
			else if(posJ + 1 < 25 && (currentBoard[posI][posJ + 1] == 'H' || currentBoard[posI][posJ + 1] == '.') && !isPoisoned(posI, posJ + 1, state))
				return Game.RIGHT;
			else
				return Game.LEFT;
		}
	}
	
	final int tryMoveRight(int posI, int posJ, Game state)
	{
		boolean poisoned = isPoisoned(posI, posJ + 1, state);
		if(!poisoned && isFalling(posI, posJ + 1))
			poisoned = isBadFall(posI, posJ + 1, state);
		if(!poisoned)
			return Game.RIGHT;
		else
		{
			if(isPoisoned(posI, posJ, state))
			{
				if(posJ - 1 >= 0 && currentBoard[posI][posJ - 1] != '=' && !isPoisoned(posI, posJ - 1, state))
					return Game.LEFT;
				else if(posI < 15 && (originalMap[posI][posJ] == 'H' || originalMap[posI + 1][posJ] == 'H') && originalMap[posI + 1][posJ] != '=' && !isPoisoned(posI + 1, posJ, state))
					return Game.BOTTOM;
				else
					return Game.TOP;
			}
			else
				return Game.NONE;
		}
	}

	final int tryMoveLeft(int posI, int posJ, Game state)
	{
		boolean poisoned = isPoisoned(posI, posJ - 1, state);
		if(!poisoned && isFalling(posI, posJ - 1))
			poisoned = isBadFall(posI, posJ - 1, state);
		if(!poisoned)
			return Game.LEFT;
		else
		{
			if(isPoisoned(posI, posJ, state))
			{
				if(posJ + 1 < 25 && currentBoard[posI][posJ + 1] != '=' && !isPoisoned(posI, posJ + 1, state))	
					return Game.RIGHT;
				else if(posI < 15 && (originalMap[posI][posJ] == 'H' || originalMap[posI + 1][posJ] == 'H') && originalMap[posI + 1][posJ] != '=' && !isPoisoned(posI + 1, posJ, state))
					return Game.BOTTOM;
				else
					return Game.TOP;
			}
			else
				return Game.NONE;
		}
	}
	
	final int zombieDefense(int posI, int posJ, Game state, int normalChoice)
	{
		if(posJ < 24 && originalMap[posI][posJ + 1] != '=' && (zombieNear(posI, posJ, 0, 1) || (zombieNear(posI, posJ, 0, 2) && zombieCanMove(posI, posJ, 0, 2, state))))
		{
			if(canDig(posI, posJ, 1, state, isRed) && ((zombieNear(posI, posJ, 0, 2) && !zombieNear(posI, posJ, 0, 1)) || !zombieCanMove(posI, posJ, 0, 1, state)))
				return Game.DIG_RIGHT;
			else
			{
				if(zombieNear(posI, posJ, 0, 1) && zombieCanMove(posI, posJ, 0, 1, state) && (!(posI >= 1 && originalMap[posI][posJ] == 'H' && originalMap[posI - 1][posJ] != '=' && normalChoice == Game.TOP)) && (!(posI < 15 && (originalMap[posI + 1][posJ] == 'H' || (originalMap[posI][posJ] == 'H' && originalMap[posI + 1][posJ] != '=')) && normalChoice == Game.BOTTOM)))
					return tryMoveLeft(posI, posJ, state);
				else if(normalChoice == Game.RIGHT)
				{
					if(zombieNear(posI, posJ, 0, 1) && !zombieCanMove(posI, posJ, 0, 1, state))
						return Game.NONE;
					else if(zombieNear(posI, posJ, 0, 2) && zombieCanMove(posI, posJ, 0, 2, state))
						return Game.NONE;
					else if(posJ >= 1 && originalMap[posI][posJ - 1] != '=')
						return tryMoveLeft(posI, posJ, state);
					else if(posI < 15 && (originalMap[posI + 1][posJ] == 'H' || (originalMap[posI][posJ] == 'H' && originalMap[posI + 1][posJ] != '=')))
						return tryMoveDown(posI, posJ, state);
					else if(posI >= 1 && originalMap[posI][posJ] == 'H' && originalMap[posI + 1][posJ] != '=')
						return tryMoveUp(posI, posJ, state);
					else
						return Game.NONE;
				}
			}
		}
		if(posJ >= 1 && originalMap[posI][posJ - 1] != '=' && (zombieNear(posI, posJ, 0, -1) || (zombieNear(posI, posJ, 0, -2) && zombieCanMove(posI, posJ, 0, -2, state))))
		{
			if(canDig(posI, posJ, -1, state, isRed) && ((zombieNear(posI, posJ, 0, -2) && !zombieNear(posI, posJ, 0, -1)) || !zombieCanMove(posI, posJ, 0, -1, state)))
				return Game.DIG_LEFT;
			else
			{
				if(zombieNear(posI, posJ, 0, -1) && zombieCanMove(posI, posJ, 0, -1, state) && (!(posI >= 1 && originalMap[posI][posJ] == 'H' && normalChoice == Game.TOP)) && (!(posI < 15 && (originalMap[posI + 1][posJ] == 'H' || (originalMap[posI][posJ] == 'H' && originalMap[posI + 1][posJ] != '=')) && normalChoice == Game.BOTTOM)))
					return tryMoveRight(posI, posJ, state);
				else if(normalChoice == Game.LEFT)
				{
					if(zombieNear(posI, posJ, 0, -1) && !zombieCanMove(posI, posJ, 0, -1, state))
						return Game.NONE;
					else if(zombieNear(posI, posJ, 0, -2) && zombieCanMove(posI, posJ, 0, -2, state))
						return Game.NONE;
					else if(posJ < 24 && originalMap[posI][posJ + 1] != '=')
						return tryMoveRight(posI, posJ, state);
					else if(posI < 15 && (originalMap[posI + 1][posJ] == 'H' || (originalMap[posI][posJ] == 'H' && originalMap[posI + 1][posJ] != '=')))
						return tryMoveDown(posI, posJ, state);
					else if(posI >= 1 && originalMap[posI][posJ] == 'H' && originalMap[posI + 1][posJ] != '=')
						return tryMoveUp(posI, posJ, state);
					else
						return Game.NONE;
				}
			}
		}
		if(normalChoice != Game.LEFT && normalChoice != Game.RIGHT && ((zombieNear(posI, posJ, 1, 0) && (!trappedZombie(posI + 1, posJ) || normalChoice == Game.BOTTOM)) || (zombieNear(posI, posJ, 2, 0) && !trappedZombie(posI + 2, posJ) && originalMap[posI + 1][posJ] == 'H' && originalMap[posI + 2][posJ] == 'H')  || (zombieNear(posI, posJ, 3, 0) && !trappedZombie(posI + 3, posJ) && originalMap[posI + 1][posJ] == 'H' && originalMap[posI + 2][posJ] == 'H' && originalMap[posI + 3][posJ] == 'H' && (posJ == 0 || originalMap[posI + 1][posJ - 1] == '=') && (posJ == 24 || originalMap[posI + 1][posJ + 1] == '='))))			
		{
			if(posJ >= 1 && originalMap[posI][posJ - 1] != '=' && (posI == 15 || originalMap[posI + 1][posJ - 1] == '=')) 
				return tryMoveLeft(posI, posJ, state);
			if(posJ < 24 && originalMap[posI][posJ + 1] != '=' && (posI == 15 || originalMap[posI + 1][posJ + 1] == '='))
				return tryMoveRight(posI, posJ, state);
			if(zombieNear(posI, posJ, 2, 0) && !zombieCanMove(posI, posJ, 2, 0, state) && ((posJ >= 1 && originalMap[posI + 1][posJ - 1] != '=' && !zombieNear(posI, posJ, 1, -1)) || (posJ < 24 && originalMap[posI + 1][posJ + 1] != '=' && !zombieNear(posI, posJ, 1, 1))))
				return tryMoveDown(posI, posJ, state);
			if(posI >= 1 && originalMap[posI][posJ] == 'H' && originalMap[posI - 1][posJ] != '=')
				return tryMoveUp(posI, posJ, state);
			if(posJ >= 1 && originalMap[posI][posJ - 1] != '=') 
				return tryMoveLeft(posI, posJ, state);
			if(posJ < 24 && originalMap[posI][posJ + 1] != '=')
				return tryMoveRight(posI, posJ, state);
			else
				return tryMoveUp(posI, posJ, state);
		}
		if(originalMap[posI][posJ] == 'H')
			if(normalChoice != Game.LEFT && normalChoice != Game.RIGHT && ((zombieNear(posI, posJ, -1, 0) && (normalChoice == Game.TOP || !trappedZombie(posI - 1, posJ))) || ((zombieNear(posI, posJ, -2, 0) && !trappedZombie(posI - 2, posJ) && originalMap[posI - 1][posJ] != '=') || (posI != 0 && isPoisoned(posI - 1, posJ, state) && normalChoice == Game.TOP)) || (zombieNear(posI, posJ, -3, 0) && !trappedZombie(posI - 3, posJ) && originalMap[posI - 1][posJ] == 'H' && originalMap[posI - 2][posJ] == 'H' && (posJ == 0 || originalMap[posI - 1][posJ - 1] == '=') && (posJ == 24 || originalMap[posI - 1][posJ + 1] == '='))))
			{
				if(posJ >= 1 && originalMap[posI][posJ - 1] != '=' && (posI == 15 || originalMap[posI + 1][posJ - 1] == '=')) 
					return tryMoveLeft(posI, posJ, state);
				if(posJ < 24 && originalMap[posI][posJ + 1] != '=' && (posI == 15 || originalMap[posI + 1][posJ + 1] == '='))
					return tryMoveRight(posI, posJ, state);
				if(zombieNear(posI, posJ, -2, 0) && !zombieCanMove(posI, posJ, -2, 0, state) && ((posJ >= 1 && originalMap[posI - 1][posJ - 1] != '=' && !zombieNear(posI, posJ, -1, -1)) || (posJ < 24 && originalMap[posI - 1][posJ + 1] != '='  && !zombieNear(posI, posJ, -1, 1))))
					return tryMoveUp(posI, posJ, state);
				if(posI < 15 && (originalMap[posI + 1][posJ] == 'H' || normalChoice == Game.BOTTOM))
					return tryMoveDown(posI, posJ, state);
				if(posJ >= 1 && originalMap[posI][posJ - 1] != '=') 
					return tryMoveLeft(posI, posJ, state);
				if(posJ < 24 && originalMap[posI][posJ + 1] != '=')
					return tryMoveRight(posI, posJ, state);
				else
					return tryMoveDown(posI, posJ, state);
			}
		if(normalChoice == Game.TOP)
			return tryMoveUp(posI, posJ, state);
		else if(normalChoice == Game.BOTTOM)
			return tryMoveDown(posI, posJ, state);
		else if(normalChoice == Game.LEFT)
			return tryMoveLeft(posI, posJ, state);
		else if(normalChoice == Game.RIGHT)
			return tryMoveRight(posI, posJ, state);
		else
			return normalChoice;
	}
}