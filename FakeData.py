import asyncio
import websockets
import json
import random

async def send_data():
    uri = "ws://localhost:8000"
    async with websockets.connect(uri) as websocket:
        while True:
            x = round(random.uniform(-1, 1), 2)
            y = round(random.uniform(-1, 1), 2)
            z = round(9.8 + random.uniform(-0.1, 0.1), 2)
            data = {"x": x, "y": y, "z": z}
            await websocket.send(json.dumps(data))
            await asyncio.sleep(0.1)

asyncio.run(send_data())
