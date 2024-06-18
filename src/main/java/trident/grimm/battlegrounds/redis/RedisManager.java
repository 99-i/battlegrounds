package trident.grimm.battlegrounds.redis;

import lombok.Getter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import trident.grimm.battlegrounds.App;

//manages everything to do with updating the server's state to redis for the hub.
public class RedisManager {

	private JedisPool pool;
	private Jedis jedis;
	private boolean connected = false;

	private static @Getter RedisManager instance = new RedisManager();

	public RedisManager() {
	}

	public void connect() {
		pool = new JedisPool("127.0.0.1", 6379);
		try {
			jedis = pool.getResource();
			connected = true;
		} catch (Exception e) {
			connected = false;
		}

	}

	public boolean isConnected() {
		return this.connected;
	}

	public void unregister() {
		if (jedis != null) {
			jedis.close();
		}
		if (pool != null) {
			pool.close();
		}
	}

	// upload either "ready" or "not-ready" as the server status.
	// this affects whether players can join the server from the hub server picker.
	public void uploadStatus(boolean status) {
		String statusString = status ? "ready" : "not-ready";
		jedis.hset(getServerInstance(), "status", statusString);
	}

	private String getServerInstance() {
		return "server:" + App.getInstance().getConfig().getString("server_id");
	}

	// get the current version string (e.g. '12.4' meaning season 12 patch 4)
	public String getCurrentVersion() {
		return jedis.get("current-season") + "." + jedis.get("current-patch");
	}

	public boolean isPreseason() {
		return jedis.get("is-preseason").equals("true");
	}
}
