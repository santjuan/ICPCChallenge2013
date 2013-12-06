public class MLTrainerTest extends Game.Strategy
{
	String fileName = "t-set.txt";
	
	public MLTrainerTest()
	{
	}
	
	public MLTrainerTest(String fN)
	{
		fileName = fN;
	}
	
	public static void main(String[] args) throws InterruptedException
	{
		RealtimeState state = new RealtimeState();
		MLTrainerTest strategy = new MLTrainerTest("t-set.txt");
		strategy.init(state, state.initial, true);
		state.readTurn(-1);
		while(state.currentTurn >= 0)
			state.readTurn(strategy.getPlay(state)); 
		((MLTrainerAdvisor) (strategy.gold)).writeAll();
		System.exit(0);
	}
	
	void writeAll()
	{
		((MLTrainerAdvisor) (gold)).writeAll();
	}

	@Override
	void init(Game state, Game.InitialState initialState, boolean isRed)
	{
		gold = new MLTrainerAdvisor(fileName);
		gold.init(initialState, state, isRed);
		defense = new BruteForceDefense(6);
		defense.init(initialState, state, isRed);
	}
}
