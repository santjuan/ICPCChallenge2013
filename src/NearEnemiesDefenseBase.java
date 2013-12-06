import java.util.Random;

public abstract class NearEnemiesDefenseBase extends ZombieDefense implements Game.DefenseAdvisor 
{
	Game.GoldAdvisor currentGoldAdvisor;
	final Random random = new Random(1000000007);

	@Override
	public abstract void init(Game.InitialState initialState, Game initialGame, boolean red);
	
	@Override
	public int getAdvice(Game game, int goldChoice, long timeLeft, Game.GoldAdvisor goldAdvisor)
	{
		canSimulate = true;
		currentGoldAdvisor = goldAdvisor;
		return getAdvice(game, goldChoice, timeLeft);
	}
	
	@Override
	public int getQuickAdvice(Game game, int goldChoice, long timeLeft, Game.GoldAdvisor goldAdvisor, boolean red) 
	{
		return getAdvice(game, goldChoice, timeLeft);
	}
	
	abstract boolean doomPredicted(Game game, int advice, int tolerance);
	
	abstract int findSavingMove(Game game, int advice, int priority);
	
	int getAdvice(Game game, int goldChoice, long timeLeft)
	{
		if(game.getPlayer(isRed).i < 0) return Simulator.NONE;
		game.fillMap(currentBoard, true, true, true);
		int advice = goldChoice;
		if(goldChoice == Game.SUICIDE)
		{
			System.err.println("Suciding");
			int posI = game.getPlayer(isRed).i;
			int posJ = game.getPlayer(isRed).j;
			if(posI < 15 && posJ >= 1 && canDig(posI, posJ, -1, game, isRed))
				advice = Game.DIG_LEFT;
			else if(posI < 15 && posJ < 24 && canDig(posI, posJ, 1, game, isRed))
				advice = Game.DIG_RIGHT;
			else if(posI < 15 && posJ >= 1 && currentBoard[posI + 1][posJ - 1] == '-')
				advice = Game.LEFT;
			else if(posI < 15 && posJ < 24 && currentBoard[posI + 1][posJ + 1] == '-')
				advice = Game.RIGHT;
			else
			{
				double rnd = random.nextDouble();
				if(rnd < 0.35)
					advice = Game.LEFT;
				else if(rnd < 0.70)
					advice = Game.RIGHT;
				else if(rnd < 0.85)
					advice = Game.TOP;
				else
					advice = Game.BOTTOM;
			}
			if(doomPredicted(game, advice, 2))
			{
				double rnd = random.nextDouble();
				if(rnd < 0.35)
					advice = Game.LEFT;
				else if(rnd < 0.70)
					advice = Game.RIGHT;
				else if(rnd < 0.85)
					advice = Game.TOP;
				else
					advice = Game.BOTTOM;
				System.err.println("Problem with suicide");
				advice = findSavingMove(game, advice, 0);
			}
			else
				System.err.println("No problem suiciding");
		}
		else
		{
			advice = zombieDefense(game.getPlayer(isRed).i, game.getPlayer(isRed).j, game, goldChoice);
			int posI = game.getPlayer(isRed).i;
			int posJ = game.getPlayer(isRed).j;
			if(canSimulate && !game.getOriginalMap().isFalling(posI, posJ) && game.getSimulator().isValid)
				if(doomPredicted(game, advice, 0))
				{
					advice = findSavingMove(game, advice, 1);
					System.err.println("Doomed, finding move");
				}
		}
		game.clearMap(currentBoard, true, true, true);
		return advice;
	}
}