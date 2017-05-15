import re
from random import randint

diceRegex = re.compile("^(\d+)(?:d(\d*)?(?:([kdx])(\d+))?)?$")

def calc_throw(s):
    m = diceRegex.match(s)
    if not m:
        raise AttributeError("invalid string: %r"%s)
    throws, sides, typ, typ_count = m.groups()
    throws = int(throws)
    # we're done if it's a literal
    if sides is None:
        return throws
    sides = int(sides or 6) # default number of sides
    all_throws = [randint(1,sides) for _ in range(throws)]
    print("throws:", all_throws)
    if typ is None:
        return sum(all_throws)
    typ_count = int(typ_count)
    if typ == "d":
        assert typ_count < throws, "Invalid string, too many drops: %r"%s
        all_throws = sorted(all_throws)[typ_count:]
        return sum(all_throws)
    if typ == "k":
        assert typ_count < throws, "Invalid string, too many keeps: %r"%s
        all_throws = sorted(all_throws)[-typ_count:]
        return sum(all_throws)
    assert typ == "x", "Programming error this should NEVER happen"
    assert 0 < typ_count <= sides, "bad limit number: %r"%s
    all_throws = [x for x in all_throws if x < typ_count]
    while(len(all_throws) < throws):
        throw = randint(1,sides+1)
        if throw < typ_count:
            all_throws.append(throw)
            print("new throws: ", all_throws)
        else:
            print("bad throw", throw)
    return sum(all_throws)
    
    
for s in ["23", "3d", "2d7", "4d8k2", "4d8d1", "4d8x1"]:
    try:
        print (calc_throw(s))
    except Exception as e:
        print(e)
