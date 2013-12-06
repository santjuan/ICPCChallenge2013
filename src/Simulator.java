public final class Simulator extends Game
{
	static final int[][] diffs = new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
	
	static final class Gold extends Game.GameGold
	{
		int existsFrom;
		
		public Gold(int i2, int j2, int e2) 
		{
			i = i2;
			j = j2;
			existsFrom = e2;
		}

		Gold clonar()
		{
			return new Gold(i, j, existsFrom);
		}

		@Override
		int existsFrom() 
		{
			return existsFrom;
		}

		@Override
		boolean exists(int currentTurn) 
		{
			return existsFrom <= currentTurn;
		}
	}
	
	static final class Enemy extends Game.GameEnemy
	{
		UtilClasses.FastStack stackOriginal;
		UtilClasses.FastStack stack;
		int existsFrom, lastMove = -1, lastI, lastJ;
		boolean moved;
		
		Enemy clonar()
		{
			Enemy n = new Enemy();
			n.stackOriginal = stackOriginal;
			n.stack = stack.clonar();
			n.i = i; n.j = j; n.startI = startI; n.startJ = startJ;
			n.existsFrom = existsFrom; n.master = master; n.lastMove = lastMove;
			return n;
		}
		
		void pushMove(int move)
		{
			int inverse = 0;
			switch(move)
			{
				case Game.TOP: inverse = Game.BOTTOM; break;
				case Game.BOTTOM: inverse = Game.TOP; break;
				case Game.LEFT: inverse = Game.RIGHT; break;
				case Game.RIGHT: inverse = Game.LEFT; break;
			}
			stack.push(inverse);
			stack.push(move);
		}
		
		int popMove(Player[] players, OriginalMap original)
		{
			if(stack.size == 0)
				stack = stackOriginal.clonar();
			int[] range = new int[2];
			if(master == -1)
				range[0] = range[1] = 5;
			else if(master == 0)
				{ range[0] = 4; range[1] = 8; }
			else
				{ range[0] = 8; range[1] = 4; }
			int closest = Integer.MAX_VALUE;
			int closestMask = 0;
			for(int k = 0; k < players.length; k++)
			{
				if(players[k].i >= 0)
				{
					int dist = original.getReversible(i, j, players[k].lastI, players[k].lastJ);
					int distR = dist & OriginalMap.POSITION_MASK;
					if(distR > range[k]) continue;
					int distP = (dist & OriginalMap.DIRECTION_MASK) >>> 16;
					if(distR < closest) { closest = distR; closestMask = distP; }
					else if(distR == closest) closestMask |= distP;
				}
			}
			if(closest == Integer.MAX_VALUE)
				return stack.pop();
			else
			{
				if(Game.IN_DEBUG)
					System.err.println("Redirecting enemy (" + i + " " + j + "): " + closestMask + " " + closest);
				boolean left = startJ < 12;
				if(left)
				{
					if((closestMask & OriginalMap.UP) != 0) pushMove(Game.TOP);
					else if((closestMask & OriginalMap.RIGHT) != 0) pushMove(Game.RIGHT);
					else if((closestMask & OriginalMap.DOWN) != 0) pushMove(Game.BOTTOM);
					else if((closestMask & OriginalMap.LEFT) != 0) pushMove(Game.LEFT);
				}
				else
				{
					if((closestMask & OriginalMap.UP) != 0) pushMove(Game.TOP);
					else if((closestMask & OriginalMap.LEFT) != 0) pushMove(Game.LEFT);
					else if((closestMask & OriginalMap.DOWN) != 0) pushMove(Game.BOTTOM);
					else if((closestMask & OriginalMap.RIGHT) != 0) pushMove(Game.RIGHT);
				}
				return stack.pop();
			}
		}

		@Override
		int existsFrom()
		{
			return existsFrom;
		}

		@Override
		boolean canMove(int currentTurn) 
		{
			return lastMove != currentTurn;
		}
	}
	
	static final class Player extends Game.GamePlayer
	{
		int existsFrom, nextDig, lastI, lastJ;
		boolean moved;
		
		Player clonar()
		{
			Player n = new Player();
			n.i = i; n.j = j; n.startI = startI; n.startJ = startJ;
			n.existsFrom = existsFrom; n.score = score; n.nextDig = nextDig;
			return n;
		}

		@Override
		int digTime(int currentTurn)
		{
			return nextDig <= (currentTurn + 1) ? 0 : nextDig - (currentTurn + 1);
		}

		@Override
		int existsFrom(int currentTurn)
		{
			return existsFrom;
		}
	}
	
	static final class Hole extends Game.GameHole
	{
		int existsUntil, madeBy;
		
		Hole(int i2, int j2, int e2, int m2)
		{
			i = i2; j = j2; existsUntil = e2; madeBy = m2;
		}
		
		Hole clonar()
		{
			return new Hole(i, j, existsUntil, madeBy);
		}

		@Override
		int existsUntil() 
		{
			return existsUntil;
		}

		@Override
		boolean madeBy(boolean red) 
		{
			int val = red ? 1 : 2;
			return (madeBy & val) != 0;
		}
	}
	
	final Gold[] golds;
	final Enemy[] enemies;
	final Player[] players;
	final Hole[] holes;
	final UtilClasses.FastBitSet visited;
	final int turnCount;
	int currentTurn;
	final UtilClasses.FastHash map;
	final OriginalMap original;
	
	Simulator(Simulator padre, UtilClasses.FastHash m)
	{
		golds = padre.golds.clone();
		for(int i = 0; i < golds.length; i++) golds[i] = golds[i].clonar();
		enemies = padre.enemies.clone();
		for(int i = 0; i < enemies.length; i++) enemies[i] = enemies[i].clonar();
		players = padre.players.clone();
		for(int i = 0; i < players.length; i++) players[i] = players[i].clonar();
		visited = padre.visited.clonar();
		turnCount = padre.turnCount;
		currentTurn = padre.currentTurn;
		map = m;
		original = padre.original;
		holes = padre.holes.clone();
		for(int i = 0; i < holes.length; i++) holes[i] = holes[i].clonar();
		setMap();
	}
	
	Simulator(Game.InitialState initial, OriginalMap o)
	{
		original = o;
		int nGolds = 0;
		for(int i = 0; i < 16; i++)
			for(int j = 0; j < 25; j++)
				if(initial.startingBoard[i][j] == '*')
					nGolds++;
		golds = new Gold[nGolds];
		int indexG = 0;
		for(int i = 0; i < 16; i++)
			for(int j = 0; j < 25; j++)
				if(initial.startingBoard[i][j] == '*')
					golds[indexG++] = new Gold(i, j, 0);
		int nEnemies = initial.enemiesStarting.length;
		enemies = new Enemy[nEnemies];
		for(int i = 0; i < nEnemies; i++)
		{
			enemies[i] = new Enemy();
			enemies[i].stackOriginal = new UtilClasses.FastStack();
			char[] program = initial.enemiesProgram[i];
			for(int j = program.length - 1; j >= 0; j--)
			{
				int action = 0;
				switch(program[j])
				{
					case 'R': action = Game.RIGHT; break;
					case 'L': action = Game.LEFT; break;
					case 'B': action = Game.BOTTOM; break;
				     default: action = Game.TOP;
				}
				enemies[i].stackOriginal.push(action);
			}
			enemies[i].startI = enemies[i].i = initial.enemiesStarting[i].i;
			enemies[i].startJ = enemies[i].j = initial.enemiesStarting[i].j;
			enemies[i].stack = enemies[i].stackOriginal.clonar();
			enemies[i].master = -1;
		}
		visited = new UtilClasses.FastBitSet();
		turnCount = initial.turnCount;
		currentTurn = -1;
		players = new Player[2];
		players[0] = new Player(); players[1] = new Player();
		players[0].startI = players[0].i = initial.redStarting.i;
		players[0].startJ = players[0].j = initial.redStarting.j;
		players[1].startI = players[1].i = initial.blueStarting.i;
		players[1].startJ = players[1].j = initial.blueStarting.j;
		players[0].score = 1; players[1].score = 1;
		visited.set(players[0].startI * 25 + players[0].startJ);
		visited.set(players[1].startI * 25 + players[1].startJ);
		map = new UtilClasses.FastHash(original.map);
		holes = new Hole[8]; 
		for(int i = 0; i < 8; i++) holes[i] = new Hole(-1, -1, -1, 0);
		setMap();
	}
	
	void setMap()
	{
		map.clear();
		for(Gold g : golds) if(g.existsFrom <= currentTurn) map.put(g.i, g.j, '*');
		for(Hole h : holes) if(h.i >= 0) map.put(h.i, h.j, '-');
	}
	
	boolean standingOnSomething(int i, int j)
	{
		if(map.originalMap[i][j] == 'H') return true;
		if(i == 15) return true;
		char down = map.originalMap[i + 1][j];
		char currentDown = map.get(i + 1, j);
		if(down == 'H') return true;
		if(down == '=' && currentDown == '=') return true;
		if(down == '=' && currentDown == '-')
			for(Enemy e : enemies) if(e.lastI == i + 1 && e.lastJ == j) return true;
		return false;
	}
	
	boolean standingOnSomethingNow(int i, int j)
	{
		if(map.originalMap[i][j] == 'H') return true;
		if(i == 15) return true;
		char down = map.originalMap[i + 1][j];
		char currentDown = map.get(i + 1, j);
		if(down == 'H') return true;
		if(down == '=' && currentDown == '=') return true;
		if(down == '=' && currentDown == '-')
			for(Enemy e : enemies) if(e.i == i + 1 && e.j == j) return true;
		return false;
	}
	
	boolean insideEnemy(int i, int j)
	{
		return i >= 0 && i < 16 && j >= 0 && j < 25 && original.map[i][j] != '=';
	}
	
	void tryEnemyMove(Enemy e, int move)
	{
		e.moved = true;
		if(map.get(e.i, e.j) == '-') return;
		if(map.originalMap[e.i][e.j] == 'H')
		{
			int nI = e.i + diffs[move][0];
			int nJ = e.j + diffs[move][1];
			if(insideEnemy(nI, nJ))
			{
				e.i = nI;
				e.j = nJ;
				e.lastMove = currentTurn;
			}
			return;
		}
		else if(move == Game.TOP) return;
		if(move == Game.BOTTOM && (e.i == 15 || original.map[e.i + 1][e.j] != 'H')) return;
		int nI = e.i + diffs[move][0];
		int nJ = e.j + diffs[move][1];
		if(insideEnemy(nI, nJ))
		{
			e.i = nI;
			e.j = nJ;
			e.lastMove = currentTurn;
		}
	}
	
	boolean insidePlayer(int i, int j, int move)
	{
		if(i >= 0 && i < 16 && j >= 0 && j < 25)
		{
			char val = map.get(i, j);
			if(val != '=')
			{
				if(val == '-')
				{
					for(Enemy e : enemies)
						if(e.i == i && e.j == j && move == Game.BOTTOM) return false;
					return true;
				}
				else return true;
			}
		}
		return false;
	}
	
	void tryPlayerMove(Player p, int move) 
	{
		p.moved = true;
		int nI = p.i + diffs[move][0];
		int nJ = p.j + diffs[move][1];
		if(!insidePlayer(nI, nJ, move)) return;
		if(original.map[p.i][p.j] == 'H') { p.i = nI; p.j = nJ; }
		else
		{
			if((original.map[nI][nJ] == 'H' && move == Game.BOTTOM) || move == Game.LEFT || move == Game.RIGHT)
				{ p.i = nI; p.j = nJ;}
		}
	}
	
	boolean canMovePlayer(Player p, int move)
	{
		int nI = p.i + diffs[move][0];
		int nJ = p.j + diffs[move][1];
		if(!insidePlayer(nI, nJ, move)) return false;
		if(original.map[p.i][p.j] == 'H') return true;
		else
		{
			if((original.map[nI][nJ] == 'H' && move == Game.BOTTOM) || move == Game.LEFT || move == Game.RIGHT)
				return true;
			else
				return false;
		}
	}
	
	void tryDig(Player p, int move, int index) 
	{
		p.moved = true;
		if(p.i == 15) return;
		char down = map.get(p.i + 1, p.j);
		if(original.map[p.i][p.j] != 'H' && down != '=' && down != 'H') return;
		int tCol = p.j + (move == Game.DIG_LEFT ? -1 : 1);
		if(tCol < 0 || tCol >= 25) return;
		char side = map.get(p.i, tCol);
		if(side == 'H' || side == '=') return;
		char target = map.get(p.i + 1, tCol);
		if(target != '=') return;
		boolean put = false;
		for(Hole h : holes)
		{
			if(h.i >= 0 && h.existsUntil == currentTurn + 24 && h.i == p.i + 1 && h.j == tCol)
			{
				h.madeBy |= (index + 1);
				put = true; break;
			}
		}
		if(!put)
			for(int i = 0; i < holes.length; i++)
			{
				if(holes[i].i < 0)
				{
					holes[i].i = p.i + 1; holes[i].j = tCol; 
					holes[i].existsUntil = currentTurn + 24;
					holes[i].madeBy = index + 1;
					break;
				}
			}
		p.nextDig = currentTurn + 10;
	}
	
	boolean canDigPlayer(Player p, int move)
	{
		if(p.i == 15) return false;
		char down = map.get(p.i + 1, p.j);
		if(original.map[p.i][p.j] != 'H' && down != '=' && down != 'H') return false;
		int tCol = p.j + (move == Game.DIG_LEFT ? -1 : 1);
		if(tCol < 0 || tCol >= 25) return false;
		char side = map.get(p.i, tCol);
		if(side == 'H' || side == '=') return false;
		char target = map.get(p.i + 1, tCol);
		if(target != '=') return false;
		return true;
	}
	
	public void simulateTurn(int[] moves)
	{
		if(currentTurn == turnCount) return;
		currentTurn++;
		if(currentTurn == turnCount) return;
		for(Enemy e : enemies)
			{ e.moved = false; e.lastI = e.i; e.lastJ = e.j; }
		for(Player p : players)
			{ p.moved = false; p.lastI = p.i; p.lastJ = p.j; p.lastGoldI = p.lastGoldJ = -1; }
		for(Enemy e : enemies)
		{
			if(e.i >= 0 && !standingOnSomething(e.i, e.j) && map.get(e.i, e.j) != '-')
			{
				e.moved = true;
				e.lastMove = currentTurn;
				e.popMove(players, original);
				e.i++;
			}
		}
		for(Player p : players)
		{
			if(p.i >= 0 && !standingOnSomething(p.i, p.j))
			{
				p.i++;
				p.moved = true;
			}
		}
		for(Enemy e : enemies)
		{
			if(e.i >= 0 && !e.moved && e.lastMove <= currentTurn - 2)
				tryEnemyMove(e, e.popMove(players, original));
		}
		for(int i = 0; i < 2; i++)
		{
			if(players[i].i >= 0 && !players[i].moved && moves[i] < Game.DIG_LEFT)
				tryPlayerMove(players[i], moves[i]);
		}
		for(int i = 0; i < 2; i++)
		{
			Player p = players[i];
			if(p.i < 0) continue;
			for(Enemy e : enemies)
			{
				if(e.lastI == p.i && e.lastJ == p.j && e.i == p.lastI && e.j == p.lastJ)
				{
					p.existsFrom = currentTurn + 50;
					p.i = p.j = -1;
					break;
				}
			}
		}
		for(int i = 0; i < 2; i++)
		{
			Player p = players[i];
			if(p.i >= 0 && !p.moved && moves[i] >= Game.DIG_LEFT && moves[i] <= Game.DIG_RIGHT && p.nextDig <= currentTurn)
				tryDig(p, moves[i], i);
		}
		for(Hole h : holes)
		{
			if(h.i >= 0 && h.existsUntil < currentTurn)
			{
				for(Enemy e : enemies)
				{
					if(e.i >= 0 && e.i == h.i && e.j == h.j)
					{
						e.i = -1;
						e.j = -1;
						e.existsFrom = currentTurn + 25;
						int master = 0;
						if((h.madeBy & 1) != 0)
						{
							players[0].score += 20;
							master |= 1;
						}
						if((h.madeBy & 2) != 0)
						{
							players[1].score += 20;
							master |= 2;
						}
						if(master == 1)
							e.master = 0;
						else if(master == 2)
							e.master = 1;
						else
							e.master = -1;
					}
				}
				int indice = 0;
				for(Player p : players)
				{
					if(p.i >= 0 && p.i == h.i && p.j == h.j)
					{
						p.i = -1;
						p.j = -1;
						p.existsFrom = currentTurn + 50;
						if((h.madeBy & 1) != 0 && indice != 0)
							players[0].score += 300;
						if((h.madeBy & 2) != 0 && indice != 1)
							players[1].score += 300;
					}
					indice++;
				}
				h.i = h.j = -1; h.existsUntil = -1; h.madeBy = 0;
			}
		}
		for(Player p : players)
			if(p.i >= 0 && !visited.get(p.i * 25 + p.j))
				p.score++;
		for(Player p : players)
			if(p.i >= 0)
				visited.set(p.i * 25 + p.j);
		for(Enemy e : enemies)
		{
			if(e.i < 0 && e.existsFrom <= currentTurn)
			{
				e.i = e.startI;
				e.j = e.startJ;
				e.stack = e.stackOriginal.clonar();
			}
		}
		for(Player p : players)
		{
			if(p.i < 0 && p.existsFrom <= currentTurn)
			{
				p.i = p.startI;
				p.j = p.startJ;
			}
		}
		for(Player p : players)
		{
			if(p.i < 0) continue;
			for(Gold g : golds)
				if(g.existsFrom <= currentTurn && g.i == p.i && g.j == p.j)
				{
					p.score += 100;
					p.lastGoldI = g.i;
					p.lastGoldJ = g.j;
				}
		}
		for(Player p : players)
		{
			if(p.i < 0) continue;
			for(Gold g : golds)
				if(g.existsFrom <= currentTurn && g.i == p.i && g.j == p.j)
					g.existsFrom = currentTurn + 150;
			for(Enemy e : enemies)
				if(e.i >= 0 && e.i == p.i && e.j == p.j)
				{
					p.i = -1;
					p.j = -1;
					p.existsFrom = currentTurn + 50;
				}
		}
		setMap(); 
	}

	Simulator clonar(UtilClasses.FastHash map)
	{
		return new Simulator(this, map);
	}

	public boolean isConsistent(RealtimeState state)
	{
		char[][] mapR = state.currentBoard;
		setMap();
		for(int i = 0; i < 16; i++)
			for(int j = 0; j < 25; j++)
				if(mapR[i][j] != map.get(i, j))
					return false;
		char[][] zombieMap = new char[16][25];
		for(int i = 0; i < 16; i++)
		{
			for(int j = 0; j < 25; j++)
			{
				boolean puso = false;
				for(RealtimeState.SimpleEnemy p : state.enemies) if(p.i == i && p.j == j) {puso = true; zombieMap[i][j] = 'Z'; break;}
				if(!puso)	zombieMap[i][j] = mapR[i][j];
			}
		}
		for(int i = 0; i < 16; i++)
		{
			for(int j = 0; j < 25; j++)
			{
				boolean puso = false;
				for(Enemy e : enemies) if(e.i == i && e.j == j) {puso = true; if(zombieMap[i][j] != 'Z') return false; break;}
				if(!puso)
				{
					if(zombieMap[i][j] != map.get(i, j))
						return false;
				}
			}
		}
		if(state.blue.i != players[1].i || state.blue.j != players[1].j || state.red.i != players[0].i || state.red.j != players[0].j) return false;
		if(state.red.score != players[0].score)
		{
			if(Math.abs(state.red.score - players[0].score) <= 400)
				players[0].score = state.red.score;
			else
				return false;
		}
		if(state.blue.score != players[1].score)
		{
			if(Math.abs(state.blue.score - players[1].score) <= 400)
				players[1].score = state.blue.score;
			else
				return false;
		}
		return true;
	}
	
	boolean isValid = true;
	
	public void chequear(RealtimeState state) 
	{
		char[][] mapR = new char[16][];
		for(int i = 0; i < 16; i++) mapR[i] = state.currentBoard[i].clone();
		int debugStart = 200;
		int debugEnd = 230;
		setMap();
		for(int i = 0; i < 16; i++)
			for(int j = 0; j < 25; j++)
				if(mapR[i][j] != map.get(i, j))
				{
					System.err.println("paila");
					for(RealtimeState.SimpleEnemy p : state.enemies) if(p.i >= 0) mapR[p.i][p.j] = 'Z';
					for(char[] c : mapR)
						System.err.println(new String(c));
					System.err.println();
					for(Enemy e : enemies) if(e.i >= 0) map.put(e.i, e.j, 'Z');
					for(int ii = 0; ii < 16; ii++)
					{
						for(int jj = 0; jj < 25; jj++)
							System.err.print(map.get(ii, jj));
						System.err.println();	
					}
					if(Game.IN_DEBUG)
						throw new RuntimeException();
					else
					{
						isValid = false;
						System.err.println("Invalid");
						return;
					}
						
				}
		char[][] zombieMap = new char[16][25];
		for(int i = 0; i < 16; i++)
		{
			for(int j = 0; j < 25; j++)
			{
				boolean puso = false;
				for(RealtimeState.SimpleEnemy p : state.enemies) if(p.i == i && p.j == j) {puso = true; if(currentTurn > debugStart && currentTurn < debugEnd && Game.IN_DEBUG) System.err.print('Z'); zombieMap[i][j] = 'Z'; break;}
				if(!puso)
				{
					if(currentTurn > debugStart && currentTurn < debugEnd && Game.IN_DEBUG) System.err.print(mapR[i][j]);
					zombieMap[i][j] = mapR[i][j];
				}
			}
			if(currentTurn > debugStart && currentTurn < debugEnd && Game.IN_DEBUG) System.err.println();
		}
		if(currentTurn > debugStart && currentTurn < debugEnd && Game.IN_DEBUG) System.err.println();
		boolean paila = false;
		for(int i = 0; i < 16; i++)
		{
			for(int j = 0; j < 25; j++)
			{
				boolean puso = false;
				for(Enemy e : enemies) if(e.i == i && e.j == j) {puso = true; if(currentTurn > debugStart && currentTurn < debugEnd && Game.IN_DEBUG) System.err.print('Z'); if(zombieMap[i][j] != 'Z') paila = true; break;}
				if(!puso)
				{
					if(currentTurn > debugStart && currentTurn < debugEnd && Game.IN_DEBUG)
						System.err.print(map.get(i, j));
					if(zombieMap[i][j] != map.get(i, j))
						paila = true;
				}
			}
			if(currentTurn > debugStart && currentTurn < debugEnd && Game.IN_DEBUG) System.err.println();
		}
		if(paila)
		{
			if(Game.IN_DEBUG)
				throw new RuntimeException();
			else
			{
				isValid = false;
				System.err.println("Invalid");
				return;
			}
		}
		if(currentTurn > debugStart && currentTurn < debugEnd && Game.IN_DEBUG) System.err.println();
		if(state.blue.i != players[1].i || state.blue.j != players[1].j || state.red.i != players[0].i || state.red.j != players[0].j)
		{
			System.err.println(state.blue.i + " " + state.blue.j + " " + players[1].i + " " + players[1].j);
			System.err.println(state.red.i + " " + state.red.j + " " + players[0].i + " " + players[0].j);
			if(Game.IN_DEBUG)
				throw new RuntimeException();
			else
			{
				isValid = false;
				System.err.println("Invalid");
				return;
			}
		}
		if(Game.IN_DEBUG)
			System.err.println(state.red.score + " " + players[0].score + " " + state.blue.score + " " + players[1].score);
		if(state.red.score != players[0].score)
		{
			if(Math.abs(state.red.score - players[0].score) <= 400)
				players[0].score = state.red.score;
			else
			{
				System.err.println(state.red.score + " " + players[0].score + " " + state.blue.score + " " + players[1].score);
				if(Game.IN_DEBUG)
					throw new RuntimeException();
				else
				{
					isValid = false;
					System.err.println("Invalid");
					return;
				}
			}
		}
		if(state.blue.score != players[1].score)
		{
			if(Math.abs(state.blue.score - players[1].score) <= 400)
				players[1].score = state.blue.score;
			else
			{
				System.err.println(state.red.score + " " + players[0].score + " " + state.blue.score + " " + players[1].score);
				if(Game.IN_DEBUG)
					throw new RuntimeException();
				else
				{
					isValid = false;
					System.err.println("Invalid");
					return;
				}
			}
		}
		for(int i = 0; i < enemies.length; i++) enemies[i].master = state.enemies[i].master;
		if(players[1].nextDig > state.blue.dig + currentTurn)
			players[1].nextDig = state.blue.dig + currentTurn;
	}
	
	void fallDown(boolean red)
	{
		while(players[red ? 0 : 1].i >= 0 && !standingOnSomethingNow(players[red ? 0 : 1].i, players[red ? 0 : 1].j) && currentTurn != turnCount)
			simulateTurn(new int[]{Game.NONE, Game.NONE});
	}
	
	int doomed(int nTurns, int[] possibleMoves, UtilClasses.FastHash hash, boolean red)
	{
		if(currentTurn == turnCount)
			return 0;
		if(nTurns == 0)
			return isDeadOrTrapped(red);
		int val = isDeadOrTrapped(red);
		if(val != 0)
			return val;
		int max = 3;
		for(int m : possibleMoves)
		{		
			if((m == Game.DIG_LEFT || m == Game.DIG_RIGHT) && (players[red ? 0 : 1].nextDig > currentTurn + 1 || !canDigPlayer(players[red ? 0 : 1], m))) continue;
			else if(m != Game.DIG_LEFT && m != Game.DIG_RIGHT && m != Game.NONE && !canMovePlayer(players[red ? 0 : 1], m)) {continue;}
			Simulator s = clonar(hash);
			s.fallDown(red);
			s.simulateTurn(new int[]{red ? m : Game.NONE, red ? Game.NONE : m});
			s.fallDown(red);
			int v = s.doomed(nTurns - 1, possibleMoves, hash, red);
			if(Game.IN_DEBUG)
				System.err.println("Trying " + m + " " + v + " " + nTurns);
			if(v == 0)
				return 0;
			max = Math.min(max, v);
		}
		return max;
	}
	
	int isDeadOrTrapped(boolean red)
	{
		int posI = getPlayer(red).i;
		int posJ = getPlayer(red).j;
		if(posI < 0) return 1;
		boolean zombieLeft = false;
		boolean zombieRight = false;
		for(Enemy e : enemies)
		{
			if(e.i == posI && e.j == posJ - 1)
				zombieLeft = true;
			if(e.i == posI && e.j == posJ + 1)
				zombieRight = true;
		}
		if(map.get(posI, posJ) == '.' && (posJ == 0 || map.get(posI, posJ - 1) == '=' || (map.get(posI, posJ - 1) == '-' && zombieLeft)) && (posJ == 24 || map.get(posI, posJ + 1) == '='  || (map.get(posI, posJ + 1) == '-' && zombieRight)) && (posI == 15 || map.get(posI + 1, posJ) == '='))
			return 100;
		if(map.get(posI, posJ) == '-' && (posJ == 0 || map.get(posI, posJ - 1) == '=' || (map.get(posI, posJ - 1) == '-' && zombieLeft)) && (posJ == 24 || map.get(posI, posJ + 1) == '='  || (map.get(posI, posJ + 1) == '-' && zombieRight)) && (posI == 15 || map.get(posI + 1, posJ) == '='))
		{
			for(Game.GameHole h : getHoles())
			{
				if(h.i == posI && h.j == posJ)
				{
					if(h.madeBy(!red))
						return 3;
					else
						return 2;
				}
			}
			return 2;
		}
		return 0;
	}

	int findSavingMove(int nTurns, int[] possibleMoves, UtilClasses.FastHash hash, boolean red, int[] oponentMoves) 
	{
		if(players[red ? 0 : 1].i < 0 || nTurns == 0)
			return -1;
		int bestMoveG = -1;
		int bestValueG = 4;
		boolean visitedOther = false;
		for(int m : possibleMoves)
		{
			int bestValue = 0;
			boolean canSome = false;
			for(int m1 : oponentMoves)
			{
				if((m == Game.DIG_LEFT || m == Game.DIG_RIGHT) && (players[red ? 0 : 1].nextDig > currentTurn + 1 || !canDigPlayer(players[red ? 0 : 1], m))) continue;
				else if(m != Game.DIG_LEFT && m != Game.DIG_RIGHT && m != Game.NONE && !canMovePlayer(players[red ? 0 : 1], m)) continue;
				// System.err.println("Trying " + m + " " + players[red ? 0 : 1].i + " " + players[red ? 0 : 1].j);
				canSome = true;
				Simulator s = clonar(hash);
				s.simulateTurn(new int[]{red ? m : m1, red ? m1 : m});
				s.fallDown(red);
				int v = s.doomed(nTurns - 1, possibleMoves, hash, red);
				System.err.println("When " + m + " " + v);
				if(v > 1) visitedOther = true;
				if(v > bestValue)
					bestValue = v;
			}
			if(bestValue == 0 && canSome)
			{
				System.err.println("OK " + m);
				return m;
			}
			if(bestValue < bestValueG && canSome)
			{
				bestValueG = bestValue;
				bestMoveG = m;
			}
		}
		System.err.println(bestValueG + " " + bestMoveG);
		return !visitedOther ? -1 : bestMoveG;
	}

	@Override
	boolean isReal() 
	{
		return false;
	}

	@Override
	GameGold[] getGolds() 
	{
		return golds;
	}

	@Override
	GameEnemy[] getEnemies() 
	{
		return enemies;
	}

	@Override
	GamePlayer getPlayer(boolean red) 
	{
		return red ? players[0] : players[1];
	}

	@Override
	GameHole[] getHoles() 
	{
		return holes;
	}

	@Override
	OriginalMap getOriginalMap() 
	{
		return original;
	}

	@Override
	int getCurrentTurn()
	{
		return currentTurn;
	}

	@Override
	Simulator getSimulator() 
	{
		return this;
	}

	@Override
	int getMissedMoves() 
	{
		return 0;
	}
}