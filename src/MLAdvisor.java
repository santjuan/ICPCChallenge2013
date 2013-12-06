import java.util.LinkedList;

public class MLAdvisor extends MLAdvisorBase
{
	static interface Regressor
	{
		double getReward(MLAdvisorBase.MLEntry entry);
	}
	
	Regressor regressor;
	
	public MLAdvisor(String fileName)
	{
		regressor = new MLRegressor(fileName);
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
		return getAdvice(game, timeLeft, defenseAdvisor);
	}
	
	int currentGold = -1;
	LinkedList <Integer> lastDistances = new LinkedList <Integer> ();
	
	int getMove(Game game) 
	{
		Game.GamePlayer player = game.getPlayer(isRed);
		if(player.i < 0)
		{
			currentGold = -1;
			lastDistances.clear();
			return Game.NONE;
		}
		int[] myDistance = game.getOriginalMap().getDiggingExact(game.getPlayer(isRed).i, game.getPlayer(isRed).j, game.getPlayer(isRed).digTime(game.getCurrentTurn()), currentBoard, currentMapHoles);
		int[] hisDistance;
		if(game.getPlayer(!isRed).i < 0)
			hisDistance = game.getOriginalMap().getDiggingExact(game.getPlayer(!isRed).startI, game.getPlayer(!isRed).startJ, 0, currentBoard, currentMapHoles);
		else
			hisDistance = game.getOriginalMap().getDiggingExact(game.getPlayer(!isRed).i, game.getPlayer(!isRed).j, game.getPlayer(!isRed).digTime(game.getCurrentTurn()), currentBoard, currentMapHoles);
		assignGold(myDistance, hisDistance, game);
		if(currentGold == -1) return Game.NONE;
		return OriginalMap.getDirection(game.getShortestRoute(myDistance, startDistanceO, currentGold, currentBoard, isRed));
	}
	
	int selectBestGold(int[] myDistance, int[] hisDistance, Game game) 
	{
		double best = Double.NEGATIVE_INFINITY;
		int bestId = 0;
		Object[] tspAns = tspAdvisor.getMove(game, 120, null);
		for(int i = 0; i < golds.length; i++)
		{
			MLAdvisorBase.MLEntry entry = new MLEntry(game, i, myDistance, hisDistance, tspAns);
			System.err.println("Gold " + golds[i][0] + " " + golds[i][1] + " " + entry.myDistance);
			double reward = regressor.getReward(entry);
			if(reward > best)
			{
				best = reward;
				bestId = i;
			}
		}
		System.err.println("Selecting " + golds[bestId][0] + " " + golds[bestId][1] + " " + best);
		return bestId;
	}
	
	void assignGold(int[] myDistance, int[] hisDistance, Game game) 
	{
		int r = selectBestGold(myDistance, hisDistance, game);
		currentGold = r;
	}
}