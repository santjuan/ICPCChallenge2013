import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.Random;

public class MLTrainerAdvisor extends MLAdvisorBase
{
	static final boolean IN_DEBUG = false;
	static final double DECREASING_FACTOR = 0.975d;
	final Random random = new Random();
	final BufferedWriter bw;
	
	MLTrainerAdvisor(String fileName)
	{
		try
		{
			bw = new BufferedWriter(new FileWriter(fileName, true));
		}
		catch(Exception e)
		{
			throw new RuntimeException();
		}
	}
	
	@Override
	public int getAdvice(Game game, long timeLeft, Game.DefenseAdvisor defenseAdvisor) 
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
		return Game.NONE;
	}
	
	class Entrada extends MLAdvisorBase.MLEntry
	{
		int firstTurn;
		int id;
		String reason;
		
		Entrada(Game game, int i, int[] myDistanceV, int[] hisDistance, Object[] tspAns, String r)
		{
			super(game, i, myDistanceV, hisDistance, tspAns);
			firstTurn = game.getCurrentTurn();
			id = i;
			reason = r;
			if(IN_DEBUG)
				System.err.println("Enqueueing " + golds[id][0] + " " + golds[id][1] + " " + getString());
		}
		
		void write()
		{
			try 
			{
				bw.write(getString() + "\n");
			    bw.flush();
			}
			catch (IOException e) 
			{
				throw new RuntimeException(e);
			}
		}
		
		String getString()
		{
			double[] vals = getVals();
			String resp = "";
			for(double v : vals)
				resp += v + " ";
			resp += reward;
			return resp;
		}
	}

	int currentGold = -1;
	ArrayDeque <Entrada> pendingEntries = new ArrayDeque <Entrada> ();
	LinkedList <Integer> lastDistances = new LinkedList <Integer> ();
	String deadRecord = "Start ";
	boolean lastDead = false;
	
	int getMove(Game game) 
	{
		Game.GamePlayer player = game.getPlayer(isRed);
		if(player.lastGoldI >= 0)
		{
			for(int i = 0; i < golds.length; i++)
				if(golds[i][0] == player.lastGoldI && golds[i][1] == player.lastGoldJ)
					propagateReward(game, i);
		}
		while(pendingEntries.size() > 10)
		{
			Entrada e = pendingEntries.pollLast();
			e.write();
		}
		if(player.i < 0)
		{
			currentGold = -1;
			lastDistances.clear();
			if(!lastDead)
				deadRecord += " died " + game.getCurrentTurn(); 
			lastDead = true;
			return Game.NONE;
		}
		lastDead = false;
		int[] myDistance = game.getOriginalMap().getDiggingExact(game.getPlayer(isRed).i, game.getPlayer(isRed).j, game.getPlayer(isRed).digTime(game.getCurrentTurn()), currentBoard, currentMapHoles);
		int[] hisDistance;
		if(game.getPlayer(!isRed).i < 0)
			hisDistance = game.getOriginalMap().getDiggingFast(game.getPlayer(!isRed).startI, game.getPlayer(!isRed).startJ, 0, currentBoard);
		else
			hisDistance = game.getOriginalMap().getDiggingExact(game.getPlayer(!isRed).i, game.getPlayer(!isRed).j, game.getPlayer(!isRed).digTime(game.getCurrentTurn()), currentBoard, currentMapHoles);
		if(currentGold == -1)
		{
			currentGold = -1;
			lastDistances.clear();
			if(deadRecord.trim().isEmpty())
				System.err.print("");
			assignGold(myDistance, hisDistance, game, deadRecord + " Was unassigned");
			if(currentGold != -1)
				deadRecord = "";
		}
		else
		{
			int newDistance = OriginalMap.getDistance(game.getShortestRoute(myDistance, startDistanceO, currentGold, currentBoard, isRed));
			boolean lastTaken = game.getPlayer(true).lastGoldI == golds[currentGold][0] && game.getPlayer(true).lastGoldJ == golds[currentGold][1];
			lastTaken |= game.getPlayer(false).lastGoldI == golds[currentGold][0] && game.getPlayer(false).lastGoldJ == golds[currentGold][1];
			if(lastTaken)
			{
				int before = currentGold;
				currentGold = -1;
				lastDistances.clear();
				assignGold(myDistance, hisDistance, game, "Non existing " + golds[before][0] + " " + golds[before][1]);
			}
			else
			{
				if(lastDistances.size() > 50 && lastDistances.get(49).intValue() < newDistance)
				{
					currentGold = -1;
					lastDistances.clear();
					assignGold(myDistance, hisDistance, game, "No progress " + pendingEntries.peekFirst().firstTurn + " " + game.getCurrentTurn());
				}
				else
					lastDistances.addFirst(newDistance);
			}
		}
		if(currentGold == -1) return Game.NONE;
		return OriginalMap.getDirection(game.getShortestRoute(myDistance, startDistanceO, currentGold, currentBoard, isRed));
	}

	void propagateReward(Game game, int idGold) 
	{
		if(IN_DEBUG)
			System.err.print("Propagating");
		for(Entrada e : pendingEntries)
		{
			int time = game.getCurrentTurn() - e.firstTurn;
			if(e.importants.get(idGold))
				e.reward += 100 * Math.pow(DECREASING_FACTOR, time);
			if(IN_DEBUG)
				System.err.print(" " + (100 * Math.pow(DECREASING_FACTOR, time)));
		}
		if(IN_DEBUG)
			System.err.println();
	}

	void assignGold(int[] myDistance, int[] hisDistance, Game game, String reason) 
	{
		Object[] tspAns = tspAdvisor.getMove(game, 120, null);
		if((((Integer) tspAns[0]).intValue()) != -1 && random.nextBoolean())
		{
			currentGold = (((Integer) tspAns[0]).intValue());
			if((((Integer) tspAns[0]).intValue()) == -1)
			{
				deadRecord += " found no gold tsp " + game.getCurrentTurn();
				return;
			}
			
		}
		else
		{
			int nExisting = 0;
			int[] existingIds = new int[golds.length];
			for(int i = 0; i < golds.length; i++)
			{
				if(OriginalMap.getDistance(golds[i][0], golds[i][1], startDistanceO) < 400)
					existingIds[nExisting++] = i;
			}
			if(nExisting == 0) 
			{
				deadRecord += " found no gold " + game.getCurrentTurn();
				return;
			}
			int r = random.nextInt(nExisting);
			currentGold = existingIds[r];
			if(IN_DEBUG)
				System.err.println("Random choice " + r + " " + golds[r][0] + " " + golds[r][1]);
		}
		pendingEntries.addFirst(new Entrada(game, currentGold, myDistance, hisDistance, tspAns, reason + " " + game.getCurrentTurn()));
	}

	void writeAll() 
	{
		while(pendingEntries.size() > 1)
		{
			Entrada e = pendingEntries.pollLast();
			e.write();
		}
		try {
			bw.flush();
			bw.close();
		}
		catch (IOException e) 
		{
			throw new RuntimeException();
		}
	}
}