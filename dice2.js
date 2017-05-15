let single = "3d6";
let lowest = "5d6d2";
let highest = "5d6k2";
let explosive = "4d6x6";
let literal = "2";
let fail = "xxxxxxxxx"
let complex = "2d9 + 3d6k2 +3d8d2- d12 +1000 - 5d4x4"

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
    rolls = [...sRolls(N, sides)]
    tot =  rolls.reduce((a,b)=>a+b)
    return {rolls, tot}
  case "d":  //drop
    assert(typeArg <= N, "Can't drop more than you have: "+expr)
    rolls = [...sRolls(N, sides)];
    tot =  rolls.sort().slice(typeArg).reduce((a,b)=>a+b);
    return {rolls, tot}
  case "k": //keep
    assert(typeArg <= N, "Can't keep more than you have: "+expr)
    rolls = [...sRolls(N, sides)];
    tot =  rolls.sort().slice(rolls.length-typeArg).reduce((a,b)=>a+b);
    return {rolls, tot}
  case "x":  //explode
    assert(typeArg > 1, "this expolde arg will run forever: "+expr)
    rolls = [...eRolls(N, sides, typeArg)];
    tot =  rolls.reduce((a,b)=>a+b);
    return {rolls, tot}
  default:
    throw ("you should not be able to get here but you did with: "+exp)
  }   
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

// <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

// var toRoll = "3d6";
// var [roll, N, sides, type, expNum, ...rest] = diceRegex.exec(toRoll);
// N = parseInt(N) // why do I have to parse N but not sides ....sheeesh 
// var allRolls = Array.from(new Array(N), _ => randInt(sides)) // one liner ofr folls
// //var allRolls = Array.from({length: N}, _ => randInt(sides))

// // experimental
// function rolls1(sides) {
//     return {
//        next: function(){
//            return {value: randInt(sides) , done: false};
//        }
//     };
// }

// // functional experiments
// function* sRolls(rolls, sides) {
//     for  (let i=0; i < rolls; i++) {
//         yield randInt(sides);
//     }
// }
// [...sRolls(5, 6)].sort().splice(2)  // => "5d6d2"
// Array.from(sRolls(5, 6)).sort().splice(2)  // => "5d6d2"


// function* f(rolls, sides, limit){
//   for (roll of rolls){
//     yield roll;
//     while (roll >= limit){
//       roll = randInt(sides);
//       yield roll;
//     }
//   }
// }


// function* eRolls(rolls, sides, limit) {
//     for (let i=0; i < rolls; i++) {
//       let roll = randInt(sides);
//       yield roll;

//       if (roll < limit) i++;
//     }
// }
// [...eRolls(5, 6, 3)]  // => "5d6e3"




// uiArr = "3d8k2 + 4d9d2 - d1 + 100".split(operatorRegex)



// // this appears to not be lazy so infinite loop problems
// function* rolls3(sides) {
//     while (true) {
//         yield randInt(sides);
//     }
// }
/// can it be this functional i.g. lazy?  might be too complicated
// this link shows how hard it is to make js iterables lazy:
// http://raganwald.com/2015/02/17/lazy-iteratables-in-javascript.html
//Array.from(compose(enough,filterVal)(rolls(9)))
module.exports = {
     dieRoll: dieRoll,
    evalRoll: evalRoll,
}
