import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class RealtimeState extends Game
{
	static final boolean IN_SYNC = true;
	
	class SimpleEnemy extends Game.GameEnemy
	{
		SimpleEnemy(int index, UtilClasses.Scanner sc)
		{
			i = sc.nextInt(); j = sc.nextInt(); master = sc.nextInt();
			startI = initial.enemiesStarting[index].i;
			startJ = initial.enemiesStarting[index].j;
		}
		@Override
		int existsFrom() 
		{
			return i < 0 ? currentTurn + 1 : currentTurn;
		}
		@Override
		boolean canMove(int currentTurn) 
		{
			return true;
		}
	}
	
	class SimpleGold extends Game.GameGold
	{
		public SimpleGold(int i2, int j2)
		{
			i = i2;
			j = j2;
		}

		@Override
		int existsFrom() 
		{
			return currentBoard[i][j] == '*' ? currentTurn : currentTurn + 150;
		}

		@Override
		boolean exists(int currentTurn) 
		{
			return existsFrom() <= currentTurn;
		}
	}
	
	class SimpleHole extends Game.GameHole
	{
		SimpleHole() 
		{
			i = -1;
			j = -1;
		}
		@Override
		int existsUntil() 
		{
			return currentTurn + 24;
		}

		@Override
		boolean madeBy(boolean red)
		{
			return true;
		}
	}
	
	static class PlayerInfo extends Game.GamePlayer
	{
		int dig;
		
		public PlayerInfo(UtilClasses.Scanner sc)
		{
			i = sc.nextInt();
			j = sc.nextInt();
			score = sc.nextInt();
			dig = sc.nextInt();
		}
		
		@Override
		int digTime(int currentTurn)
		{
			return dig;
		}

		@Override
		int existsFrom(int currentTurn)
		{
			return currentTurn + 50;
		}
	}
	
	static final UtilClasses.Scanner sc = new UtilClasses.Scanner();
	Game.InitialState initial;
	int n;
	int currentTurn;
	char[][] currentBoard;
	PlayerInfo red;
	PlayerInfo blue;
	final SimpleEnemy[] enemies;
	SimpleGold[] golds;
	Simulator simulator;
	final OriginalMap original;
	UtilClasses.FastHash simHash;
	AtomicBoolean isReading = new AtomicBoolean(false);
	AtomicInteger missedMoves = new AtomicInteger(0);
	
	public RealtimeState()
	{
		initial = new Game.InitialState(sc);
		n = initial.enemiesProgram.length;
		enemies = new SimpleEnemy[n];
		original = new OriginalMap(initial.startingBoard, true);
		simulator = new Simulator(initial, original);
		golds = new SimpleGold[simulator.golds.length];
		for(int i = 0; i < simulator.golds.length; i++) golds[i] = new SimpleGold(simulator.golds[i].i, simulator.golds[i].j);
		simHash = new UtilClasses.FastHash(original.map);
		final UtilClasses.Scanner scanner = sc;
		final int nEnemies = n;
		final AtomicBoolean reading = isReading;
		Thread lector = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				while(true)
				{
					ReadInfo leido = new ReadInfo(scanner, nEnemies, reading);
					readStates.add(leido);
					reading.set(false);
					int turnoLeido = leido.turn;
					long limite = turnoLeido == 0 ? 750 : 110;
					if(IN_SYNC) limite = 2000000000;
					Integer respuesta = Game.NONE;
					boolean missed = false;
					try 
					{
						respuesta = respuestas.poll(limite, TimeUnit.MILLISECONDS);
					} 
					catch (InterruptedException e)
					{
						respuestasEnviadas.add(Game.NONE);
						respuesta = Game.NONE;
						missed = true;
					}
					if(respuesta == null)
					{
						respuestasEnviadas.add(Game.NONE);
						respuesta = Game.NONE;
						missed = true;
					}
					System.out.println(RealtimeState.this.toString(respuesta.intValue()));
					System.out.flush();
					if(missed)
					{
						missedMoves.incrementAndGet();
						System.err.println("Missed move");
					}
				}
			}
		});
		lector.setDaemon(true);
		lector.setPriority(Thread.MAX_PRIORITY);
		lector.start();
	}
	
	boolean checkConsistency(int[] moves) 
	{
		Simulator sim = simulator.clonar(simHash);
		sim.simulateTurn(moves);
		return sim.isConsistent(this);
	}
	

	String toString(int play) 
	{
		switch(play)
		{
			case Game.LEFT: return "LEFT";
			case Game.RIGHT: return "RIGHT";
			case Game.TOP: return "TOP";
			case Game.BOTTOM: return "BOTTOM";
			case Game.DIG_LEFT: return "DIG_LEFT";
			case Game.DIG_RIGHT: return "DIG_RIGHT";
			default: return "NONE";
		}
	}
	
	class ReadInfo
	{
		int turn;
		char[][] board = new char[16][];
		PlayerInfo red;
		PlayerInfo blue;
		SimpleEnemy[] enemies;
		
		ReadInfo(UtilClasses.Scanner sc, int nEnemies, AtomicBoolean reading)
		{
			turn = sc.nextInt();
			reading.set(true);
			if(turn < 0) return;
			for(int i = 0; i < 16; i++) board[i] = sc.next().toCharArray();
			red = new PlayerInfo(sc);
			blue = new PlayerInfo(sc);
			enemies = new SimpleEnemy[nEnemies];
			for(int i = 0; i < nEnemies; i++) enemies[i] = new SimpleEnemy(i, sc);
		}
	}
	
	final ArrayBlockingQueue <ReadInfo> readStates = new ArrayBlockingQueue <ReadInfo> (10000, true);
	final ArrayBlockingQueue <Integer> respuestas = new ArrayBlockingQueue <Integer> (10000, true);
	final ArrayBlockingQueue <Integer> respuestasEnviadas = new ArrayBlockingQueue <Integer> (10000, true);
	int lastI = -1;
	int lastJ = -1;
	static long lastSend = System.currentTimeMillis();
	
	void readTurn(int play)
	{
		boolean wasOk = respuestasEnviadas.isEmpty() || play < 0;
		if(play >= 0)
		{
			if(wasOk)
				respuestas.add(play);
			lastSend = System.currentTimeMillis();
			lastI = blue.i;
			lastJ = blue.j;
		}
		if(!wasOk)
			play = respuestasEnviadas.poll();
		ReadInfo information;
		try
		{
			information = readStates.take();
		} 
		catch (InterruptedException e)
		{
			throw new RuntimeException(e);
		}
		currentTurn = information.turn;
		if(currentTurn == -1)
			return;
		currentBoard = information.board;
		red = information.red;
		blue = information.blue;
		for(int i = 0; i < n; i++)
			enemies[i] = information.enemies[i];
		if(play < 0)
		{
			lastI = blue.i;
			lastJ = blue.j;
		}
		if(play >= 0)
		{
			int contrary = Game.NONE;
			if(lastI >= 0)
			{
				int curI = blue.i;
				int curJ = blue.j;
				if(curI < 0)
				{
					if(checkConsistency(new int[]{play, Game.RIGHT}))
						contrary = Game.RIGHT;
					else if(checkConsistency(new int[]{play, Game.LEFT}))
						contrary = Game.LEFT;
					else if(checkConsistency(new int[]{play, Game.TOP}))
						contrary = Game.TOP;
					else if(checkConsistency(new int[]{play, Game.BOTTOM}))
						contrary = Game.BOTTOM;
					else if(checkConsistency(new int[]{play, Game.DIG_LEFT}))
						contrary = Game.DIG_LEFT;
					else if(checkConsistency(new int[]{play, Game.DIG_RIGHT}))
						contrary = Game.DIG_RIGHT;
				}
				else
				{
					if(lastI + 1 == curI)
						contrary = Game.BOTTOM;
					else if(lastI - 1 == curI)
						contrary = Game.TOP;
					else if(lastJ + 1 == curJ)
						contrary = Game.RIGHT;
					else if(lastJ - 1 == curJ)
						contrary = Game.LEFT;
					else
					{
						if(curI < 15 && curJ < 24 && checkConsistency(new int[]{play, Game.DIG_RIGHT}))
							contrary = Game.DIG_RIGHT;
						else if(curI < 15 && curJ > 0 && checkConsistency(new int[]{play, Game.DIG_LEFT}))
							contrary = Game.DIG_LEFT;
					}
				}
			}
			simulator.simulateTurn(new int[]{play, contrary});
			simulator.chequear(this);
		}
		if(!wasOk)
		{
			System.err.println("Missed move " + currentTurn);
			readTurn(Game.NONE);
		}
	}

	@Override
	int getCurrentTurn() 
	{
		return simulator.isValid ? simulator.getCurrentTurn() : currentTurn;
	}

	@Override
	boolean isReal()
	{
		return true;
	}

	@Override
	GameGold[] getGolds() 
	{
		return simulator.isValid ? simulator.getGolds() : golds;
	}

	@Override
	GameEnemy[] getEnemies() 
	{
		return simulator.isValid ? simulator.getEnemies() : enemies;
	}

	@Override
	GamePlayer getPlayer(boolean redP) 
	{
		return simulator.isValid ? simulator.getPlayer(redP) : redP ? red : blue; 
	}

	@Override
	GameHole[] getHoles() 
	{
		if(simulator.isValid)
			return simulator.getHoles();
		else
		{
			SimpleHole[] holes = new SimpleHole[8];
			for(int i = 0; i < holes.length; i++) holes[i] = new SimpleHole();
			int indice = 0;
			for(int i = 0; i < 16; i++)
				for(int j = 0; j < 25; j++)
				{
					if(currentBoard[i][j] == '-')
					{
						holes[indice].i = i;
						holes[indice++].j = j;
					}
				}
			return holes;
		}
			
	}

	@Override
	OriginalMap getOriginalMap() 
	{
		return original;
	}

	@Override
	Simulator getSimulator()
	{
		return simulator;
	}

	@Override
	int getMissedMoves() 
	{
		return missedMoves.get();
	}
}