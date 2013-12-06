import java.util.Random;

public class NearGoldNearEnemyTestResponse extends Game.Strategy
{
	public static void main(String[] args) throws InterruptedException
	{
		RealtimeState state = new RealtimeState();
		NearGoldNearEnemyTestResponse strategy = new NearGoldNearEnemyTestResponse();
		strategy.init(state, state.initial, true);
		Thread.sleep(10000);
		state.readTurn(-1);
		Random r = new Random();
		while(state.currentTurn >= 0)
		{
			if(r.nextInt(30) == 0)
				Thread.sleep(100 * r.nextInt(50));
			state.readTurn(strategy.getPlay(state));
		}
		System.exit(0);
	}

	@Override
	void init(Game state, Game.InitialState initialState, boolean isRed)
	{
		gold = new NearGoldAdvisor();
		gold.init(initialState, state, isRed);
		defense = new BruteForceDefense(6);
		defense.init(initialState, state, isRed);
	}
}
