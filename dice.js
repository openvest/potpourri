
// matches one roll spec
const diceRegex = /^(\d+)$|^(\d*)d(\d+)(?:([kdx])(\d+))?$/;
const operatorRegex = / *([-+]) */;

// rand int is 1-max inclusive e.g. 1 roll of n sided die
const randInt =  (max) =>  Math.ceil(Math.random() * max);
 
// multiple rolls of a die
function* sRolls(rolls, sides) {
    for  (let i=0; i < rolls; i++) {
        yield randInt(sides);
    }
}
//[...sRolls(5, 6)].sort().splice(2)  // => "5d6d2"

// "exploding" rolls of die if value is >= limit
function* eRolls(rolls, sides, limit) {
    for (let i=0; i < rolls;) {
      let roll = randInt(sides);
      yield roll;
      if (roll < limit) i++;
    }
}
//[...eRolls(5, 6, 3)]  // => "5d6e3"

var dieRoll = (expr) => {
  let match =  diceRegex.exec(expr);
  if (match)
    var [rolls, literal, N, sides, type, typeArg, ...rest] = match, tot;
  else
    throw("could not parse element:"+expr)
  // maybe just a literal
  if (literal)
    return {rolls: [], tot: parseInt(literal)}
  // ok we're going to roll ... at least once
  N = (N.length > 0) ? parseInt(N) : 1;
  sides = parseInt(sides);  // optional line??
  switch (type) {
  case undefined: //plain roll
    rolls = [...sRolls(N, sides)];
    break;
  case "d":  //drop
    assert(typeArg <= N, "Can't drop more than you have: "+expr);
    rolls = [...sRolls(N, sides)];
    rolls =  rolls.sort().slice(typeArg);
    break;
  case "k": //keep
    assert(typeArg <= N, "Can't keep more than you have: "+expr);
    rolls = [...sRolls(N, sides)];
    rolls = rolls.sort().slice(rolls.length-typeArg);
    break;
  case "x":  //explode
    assert(typeArg > 1, "this expolde arg will run forever: "+expr)
    rolls = [...eRolls(N, sides, typeArg)];
    break;
  default:
    throw ("you should not be able to get here but you did with: "+exp)
  }   
  tot =  rolls.reduce((a,b)=>a+b)
  return {rolls, tot}
}

const evalRoll = (expression) => {
  let uiArr = expression.split(/ *([-+]) */)
  // lets start with the first roll (or literal)
  let roll = dieRoll(uiArr[0]);
  let rolls = [roll.rolls];
  let tot = roll.tot;
  // keep rolling while there is more
  for (let i=1; i<uiArr.length;){
    let plusminus = uiArr[i++];
    roll = dieRoll(uiArr[i++]);
    rolls.push(roll.rolls);
    if (plusminus == "+")
      tot += roll.tot;
    else if (plusminus == "-")
      tot -= roll.tot;
    else
      throw("bad operator: "+plusminus);
  }
  return {rolls, tot}
}

module.exports = {
     dieRoll: dieRoll,
    evalRoll: evalRoll,
}

// test it out
evalRoll("2d9 + 3d6k2 +3d8d2- d12 +1000 - 5d4x4")
