db = db.getSiblingDB('tasksdb');
db.tasks.insertMany([
  { id: 1, name: 'Buy groceries', status: 'pending' },
  { id: 2, name: 'Walk the dog', status: 'pending' },
  { id: 3, name: 'Do laundry', status: 'completed' },
  { id: 4, name: 'Cook dinner', status: 'pending' }
]);
console.log('✅ Initial tasks seeded by Mongo init script');