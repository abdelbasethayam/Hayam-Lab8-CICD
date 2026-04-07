const express = require('express');
const { MongoClient } = require('mongodb');

const app = express();
const port = 3000;
const mongoUri = process.env.MONGO_URI || 'mongodb://db:27017/tasksdb';

let db;

const sampleTasks = [
  { id: 1, name: 'Buy groceries', status: 'pending' },
  { id: 2, name: 'Walk the dog', status: 'pending' },
  { id: 3, name: 'Do laundry', status: 'completed' },
  { id: 4, name: 'Cook dinner', status: 'pending' },
  { id: 7, name: 'Tea', status: 'pending' }     // ← NEW TASK
];

async function connectDB() {
  const client = new MongoClient(mongoUri);
  await client.connect();
  db = client.db('tasksdb');
  console.log('✅ Connected to MongoDB');

  // Re-seed every time the app starts (so Task 3 change is visible automatically)
  await db.collection('tasks').deleteMany({});
  await db.collection('tasks').insertMany(sampleTasks);
  console.log('✅ Tasks seeded');
}

app.get('/tasks', async (req, res) => {
  const tasks = await db.collection('tasks').find().toArray();
  res.json(tasks);
});

(async () => {
  await connectDB();
  app.listen(port, () => {
    console.log(`🚀 App listening on port ${port}`);
  });
})();