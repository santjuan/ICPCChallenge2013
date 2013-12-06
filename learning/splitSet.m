X = load('datos.txt');
qTrain = 0.7;
qCVal = 0.15;
%SPLITSET Splits a data set into 3 sets [training, cross_val, test]
%The split depends on the percentages specified by qTest and qCVal

Xtrain = [];
XCVal = []; 
Xtest = [];

[n,m] = size(X);
randX = randperm(n)';
nt = floor(n*qTrain);
nv = nt + floor(n*qCVal);
	
Xtrain = X(randX(1:nt,:),1:m-1);
Ytrain = X(randX(1:nt,:),m);
if (nv > nt)
    XCVal = X(randX(nt + 1:nv,:),1:m-1);
    YCVal = X(randX(nt + 1:nv,:),m);
end
if (nv < length(randX))
    Xtest  = X(randX(nv + 1:end ,:),1:m-1);
    Ytest = X(randX(nv + 1:end ,:),m);
end

save("-binary","datos.mat","Xtrain","XCVal","Xtest","Ytrain","YCVal","Ytest");
